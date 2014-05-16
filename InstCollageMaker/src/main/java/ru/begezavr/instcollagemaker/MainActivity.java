package ru.begezavr.instcollagemaker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import ru.begezavr.instcollagemaker.photo.InstaPhoto;
import ru.begezavr.instcollagemaker.photo.InstaPhotoComparatorByLikes;
import ru.begezavr.instcollagemaker.service.InstaService;
import ru.begezavr.instcollagemaker.service.InstaServiceClientCallbackInterface;


public class MainActivity extends Activity implements InstaServiceClientCallbackInterface {
    public static final String INTENT_KEY_LOADED_PHOTOS = "intent_key_loaded_photos";
    public static final String INTENT_KEY_LOAD_ERROR_MESSAGE = "intent_key_load_error_message";
    private InstaService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonGetCollage = (Button)findViewById(R.id.buttonGetCollage);
        buttonGetCollage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freezeInterface();

                //read username
                final EditText editTextInstaUser = (EditText)findViewById(R.id.editInstagramUsername);
                if(editTextInstaUser != null && editTextInstaUser.getText() != null) {
                    String instaUser = editTextInstaUser.getText().toString();

                    //todo check is correct username
                    //todo check is network available
                    if (mService != null) {
                        mService.topPhotosRequest(instaUser);
                    }
                }
            }
        });

        //service
        Intent intent = new Intent(this, InstaService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(mService != null) {
            mService.registerClient(MainActivity.this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mService != null) {
            mService.unRegisterClient();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder binder) {
        	mService = ((InstaService.LocalBinder) binder).getService();
            mService.registerClient(MainActivity.this);
        }
    	@Override
        public void onServiceDisconnected(ComponentName className) {
            mService.unRegisterClient();
            mService = null;
        }
    };

    @Override
    public void onPhotosLoaded(ArrayList<InstaPhoto> photos) {
        if(photos != null) {
            if(!photos.isEmpty()) {
                //sort by likes
                Collections.sort(photos, new InstaPhotoComparatorByLikes());
                Collections.reverse(photos);

                //show to user previews
                Intent intent = new Intent(this, PhotosPreviewActivity.class);
                intent.putExtra(PhotosPreviewActivity.INTENT_KEY_PHOTOS_ARRAY, photos);
                startActivity(intent);
            }
            else {
                Toast.makeText(this, getString(R.string.user_has_no_photos), Toast.LENGTH_LONG).show();
            }
        }
        unFreezeInterface();
    }

    @Override
    public void onPhotosLoadError(String message) {
        if(message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
        }
        unFreezeInterface();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            if (intent.hasExtra(INTENT_KEY_LOADED_PHOTOS)) {
                ArrayList<InstaPhoto> photos = null;
                try {
                    photos = (ArrayList<InstaPhoto>) intent.getSerializableExtra(INTENT_KEY_LOADED_PHOTOS);
                }
                catch (ClassCastException e) {
                    e.printStackTrace();
                }
                if(photos != null) {
                    onPhotosLoaded(photos);
                }
            }
        }
    }

    private void freezeInterface() {
        Button buttonGetCollage = (Button)findViewById(R.id.buttonGetCollage);
        buttonGetCollage.setEnabled(false);
        buttonGetCollage.setText(R.string.please_wait);
        findViewById(R.id.editInstagramUsername).setEnabled(false);
    }

    private void unFreezeInterface() {
        Button buttonGetCollage = (Button)findViewById(R.id.buttonGetCollage);
        buttonGetCollage.setEnabled(true);
        buttonGetCollage.setText(R.string.get_collage);
        findViewById(R.id.editInstagramUsername).setEnabled(true);
    }
}

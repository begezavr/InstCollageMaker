package ru.begezavr.instcollagemaker;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import ru.begezavr.instcollagemaker.photo.InstaPhoto;

public class PickPhotoActivity extends ListActivity {
    public static final String INTENT_KEY_PHOTOS_ARRAY = "intent_key_photos_array";
    public static final String INTENT_KEY_PICKED_PHOTO = "intent_key_picked_photo";

    ArrayList<InstaPhoto> mPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_photo);

        addPhotosFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        addPhotosFromIntent(intent);
    }

    @SuppressWarnings("unchecked")
    private void addPhotosFromIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(INTENT_KEY_PHOTOS_ARRAY)) {
                try {
                    mPhotos = (ArrayList<InstaPhoto>) intent.getSerializableExtra(INTENT_KEY_PHOTOS_ARRAY);
                    if(mPhotos != null) {
                        ItemsAdapter adapter = new ItemsAdapter(this, R.layout.pick_photo_list_item, mPhotos);
                        setListAdapter(adapter);
                    }
                }
                catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private class ItemsAdapter extends ArrayAdapter<InstaPhoto> {

        private ArrayList<InstaPhoto> items;

        public ItemsAdapter(Context context, int textViewResourceId, ArrayList<InstaPhoto> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.pick_photo_list_item, null);
            }
            if(v != null) {
                InstaPhoto it = items.get(position);
                if (it != null) {
                    ImageView iv = (ImageView) v.findViewById(R.id.pickPhotoListItemImage);
                    if (iv != null) {
                        Picasso.with(PickPhotoActivity.this).load(it.low.url).placeholder(R.drawable.placeholder).into(iv);
                    }
                }
            }
            return v;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(mPhotos != null && mPhotos.size() > position) {
            Bundle b = new Bundle();
            b.putSerializable(INTENT_KEY_PICKED_PHOTO, mPhotos.get(position));
            Intent intent = getIntent(); //gets the intent that called this intent
            intent.putExtras(b);
            setResult(Activity.RESULT_OK, intent);
        }
        else {
            setResult(Activity.RESULT_CANCELED);
        }
        finish();
    }
}

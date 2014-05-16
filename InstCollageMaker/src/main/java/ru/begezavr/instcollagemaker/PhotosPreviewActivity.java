package ru.begezavr.instcollagemaker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.begezavr.instcollagemaker.photo.InstaPhoto;

public class PhotosPreviewActivity extends Activity {
    public static final String INTENT_KEY_PHOTOS_ARRAY = "intent_key_photos_array";

    public final int RESULT_IMAGE_WIDTH = 1000;

    ArrayList<InstaPhoto> mPhotos;
    ArrayList<PicassoTarget> mTargetImagesOnCollage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos_collage_1);

        mTargetImagesOnCollage = new ArrayList<PicassoTarget>();
        ViewGroup collageLayout = (ViewGroup) findViewById(R.id.collageLayout);
        for(int i=0; i < collageLayout.getChildCount(); ++i) {
            try {
                ImageView iv = (ImageView) collageLayout.getChildAt(i);
                if (iv != null) {
                    mTargetImagesOnCollage.add(new PicassoTarget(iv));
                    iv.setOnClickListener(new ImageClickListener(i));
                }
            }
            catch(ClassCastException ignored) {}
        }

        final Button buttonSendEmail = (Button)findViewById(R.id.buttonSendEmail);
        buttonSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get bitmap
                try {
                    ViewGroup collageLayout = (ViewGroup) findViewById(R.id.collageLayout);
                    float scale = ((float) RESULT_IMAGE_WIDTH) / collageLayout.getWidth();
                    int resultWidth = (int) (scale * collageLayout.getWidth());
                    int resultHeight = (int) (scale * collageLayout.getHeight());
                    Bitmap bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bitmap);

                    int[] parentLocation = new int[2];
                    collageLayout.getLocationInWindow(parentLocation);

                    //collageLayout.draw(canvas) - ну очень быстрое решение конечно, но не очень подходит, так как результат получается зависящим от разрешения устройства
                    //draw images
                    for (int i = 0; i < collageLayout.getChildCount(); ++i) {
                        try {
                            ImageView iv = (ImageView) collageLayout.getChildAt(i);
                            if(iv != null && iv.getDrawable() != null) {
                                int[] location = new int[2];
                                iv.getLocationInWindow(location);

                                Bitmap b = ((BitmapDrawable)iv.getDrawable()).getBitmap();
                                int x = (int) ((location[0] - parentLocation[0]) * scale);
                                int y = (int) ((location[1] - parentLocation[1]) * scale);
                                int width = (int) (iv.getWidth() * scale);
                                int height = (int) (iv.getHeight() * scale);

                                Bitmap croppedB = scaleCenterCrop(b, height, width);
                                canvas.drawBitmap(croppedB, new Rect(0, 0, croppedB.getWidth(), croppedB.getHeight()), new Rect(x, y, x+width, y+height), null);
                            }
                        }
                        catch(ClassCastException ignored){}
                    }

                    //save to file
                    FileOutputStream out = null;
                    try {
                        File dir = getExternalCacheDir();
                        if(dir != null) {
                            String filename = dir.getAbsolutePath() + "/collage.jpg";
                            out = new FileOutputStream(filename);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            sendEmail(filename);
                        }
                        //todo else {say about else}
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        if(out != null) {
                            try {
                                out.close();
                            }
                            catch (IOException ignore) {}
                        }
                    }
                }
                catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        });


        showPhotosFromIntent(getIntent());
    }

    private void sendEmail(String filename) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        //        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailSignature);
        //emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, toSenders);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "А ну ка коллаж");
        //emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, messageText+"\n\n"+emailSignature);

        emailIntent.setType("image/jpeg");
        File bitmapFile = new File(filename);
        Uri myUri = Uri.fromFile(bitmapFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, myUri);

        startActivity(Intent.createChooser(emailIntent, "Отправить письмо с помощью:"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showPhotosFromIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null) {
            return;
        }
        if(resultCode == Activity.RESULT_OK) {
            try {
                InstaPhoto picked = (InstaPhoto) data.getSerializableExtra(PickPhotoActivity.INTENT_KEY_PICKED_PHOTO);
                if(picked != null && requestCode < mTargetImagesOnCollage.size()) {
                    Picasso.with(this).load(picked.standard.url).placeholder(R.drawable.placeholder).into(mTargetImagesOnCollage.get(requestCode));
                }
            }
            catch (ClassCastException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void showPhotosFromIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(INTENT_KEY_PHOTOS_ARRAY)) {
                try {
                    mPhotos = (ArrayList<InstaPhoto>) intent.getSerializableExtra(INTENT_KEY_PHOTOS_ARRAY);
                    if(mPhotos != null) {
                        ViewGroup collageLayout = (ViewGroup) findViewById(R.id.collageLayout);
                        for(int i=0; i < collageLayout.getChildCount() && i < mPhotos.size(); ++i) {
                            try {
                                try {
                                    Picasso.with(this).load(mPhotos.get(i).standard.url).into(mTargetImagesOnCollage.get(i));
                                }
                                catch(Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            catch (ClassCastException ignored) {}
                        }
                    }
                }
                catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //http://stackoverflow.com/questions/8112715/how-to-crop-bitmap-center-like-imageview
    public Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    class ImageClickListener implements View.OnClickListener {
        private int position;
        ImageClickListener(int pos) {
            this.position = pos;
        }
        @Override
        public void onClick(View v) {
            //start activity with other images to pick
            Intent intent = new Intent(PhotosPreviewActivity.this, PickPhotoActivity.class);

            int maxElementToPick = 20;
            if(maxElementToPick >= mPhotos.size()) {
                maxElementToPick = mPhotos.size() - 1;
            }
            List<InstaPhoto> tempList = mPhotos.subList(0, maxElementToPick);
            ArrayList<InstaPhoto> reducedList = new ArrayList<InstaPhoto>(tempList);
            intent.putExtra(PhotosPreviewActivity.INTENT_KEY_PHOTOS_ARRAY, reducedList);
            startActivityForResult(intent, this.position);
        }
    }

    //пришлось использовать Target так как мне нужно доставать битмапы при сборке вьюшек в итоговую картинку-коллаж
    private class PicassoTarget implements Target {
        private ImageView imageView;
        PicassoTarget(ImageView iv) {
            imageView = iv;
        }
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            imageView.setImageBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            //todo set error drawable
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            Drawable res = getResources().getDrawable(R.drawable.placeholder);
            imageView.setImageDrawable(res);
        }
    }
}

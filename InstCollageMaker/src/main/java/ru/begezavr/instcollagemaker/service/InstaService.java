package ru.begezavr.instcollagemaker.service;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import ru.begezavr.instcollagemaker.InstaConnectionParams;
import ru.begezavr.instcollagemaker.MainActivity;
import ru.begezavr.instcollagemaker.R;
import ru.begezavr.instcollagemaker.photo.InstaImage;
import ru.begezavr.instcollagemaker.photo.InstaPhoto;
import ru.begezavr.instcollagemaker.tools.NetworkTools;

/**
 * Service for receive data from Instagram API
 */
public class InstaService extends Service {
    InstaServiceClientCallbackInterface mClient;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public InstaService getService() {
            // Return this instance of AegisService so clients can call public methods
            return InstaService.this;
        }
    }

    //Methods for clients
    public void registerClient(InstaServiceClientCallbackInterface client) {
        mClient = client;
    }

    public void unRegisterClient() {
        mClient = null;
    }

    public void topPhotosRequest(String user_id) {
        new GetTopPhotosTask().execute(user_id);
    }

    private class GetTopPhotosTask extends AsyncTask<String, Void, ArrayList<InstaPhoto>> {
        private String mErrorMessage;

        @Override
        protected void onPostExecute(ArrayList<InstaPhoto> result) {
            if(mClient != null) {
                if(result == null) {
                    mClient.onPhotosLoadError(mErrorMessage);
                }
                else {
                    mClient.onPhotosLoaded(result);
                }
            }
            else {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.INTENT_KEY_LOADED_PHOTOS, result);
                intent.putExtra(MainActivity.INTENT_KEY_LOAD_ERROR_MESSAGE, mErrorMessage);
                Application app = getApplication();
                if(app != null) {
                    app.startActivity(intent);
                }
            }
        }
        @Override
        protected ArrayList<InstaPhoto> doInBackground(String... req) {
            ArrayList<InstaPhoto> result = null;
            try {
                /****
                 Request params
                 ****/
                String userName = req[0];
                int maxPhotosToLoad = 500;

                //get id
                String userId = getUserId(userName);
                if(userId != null) {
                    String getAllMediaUrl = "https://api.instagram.com/v1/users/" + userId + "/media/recent/?client_id=" + InstaConnectionParams.CLIENT_ID + "&min_id=0";

                    result = new ArrayList<InstaPhoto>();
                    boolean continueRequests = true;
                    while(continueRequests) {
                        //todo send progress to activity
                        continueRequests = false;
                        /****
                         Do request
                         ****/
                        URL url = new URL(getAllMediaUrl);
                        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                        //urlConnection.setHostnameVerifier(DO_NOT_VERIFY);
                        //urlConnection.setReadTimeout(15000);
                        //urlConnection.setConnectTimeout(2000);
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setDoOutput(false);

                        urlConnection.connect();
                        String response = null;
                        try {
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            response = NetworkTools.convertStreamToString(in);
                        } finally {
                            urlConnection.disconnect();
                        }
                        if (response != null) {
                            try {
                                JSONObject json = new JSONObject(response);
                                if (json.has("data")) {
                                    JSONArray jsPhotos = json.getJSONArray("data");
                                    for (int i = 0; i < jsPhotos.length(); ++i) {
                                        JSONObject jsPhoto = jsPhotos.getJSONObject(i);
                                        if(jsPhoto.getString("type").equalsIgnoreCase("image")) {
                                            JSONObject jsImages = jsPhoto.getJSONObject("images");
                                            JSONObject jsLow = jsImages.getJSONObject("low_resolution");
                                            JSONObject jsThumb = jsImages.getJSONObject("thumbnail");
                                            JSONObject jsStandard = jsImages.getJSONObject("standard_resolution");
                                            InstaPhoto photo = new InstaPhoto();
                                            photo.low = new InstaImage(jsLow.getString("url"), jsLow.getInt("width"), jsLow.getInt("height"));
                                            photo.thumb = new InstaImage(jsThumb.getString("url"), jsThumb.getInt("width"), jsThumb.getInt("height"));
                                            photo.standard = new InstaImage(jsStandard.getString("url"), jsStandard.getInt("width"), jsStandard.getInt("height"));
                                            photo.likes = jsPhoto.getJSONObject("likes").getInt("count");
                                            result.add(photo);
                                        }
                                    }
                                }
                                if (json.has("pagination") && result.size() < maxPhotosToLoad) {
                                    getAllMediaUrl = json.getJSONObject("pagination").getString("next_url");
                                    continueRequests = true;
                                }
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            catch(MalformedURLException e) {
                e.printStackTrace();
            }
            catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            catch(ProtocolException e) {
                e.printStackTrace();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(NullPointerException e) {
                //android bug: http://code.google.com/p/android/issues/detail?id=16895
                e.printStackTrace();
            }
            return result;
        }

        private String getUserId(String userName) {
            String userId = null;

            try {
                String getUserUrl = "https://api.instagram.com/v1/users/search?q=" + userName + "&client_id=" + InstaConnectionParams.CLIENT_ID + "&count=1";
                URL url = new URL(getUserUrl);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                //urlConnection.setReadTimeout(15000);
                //urlConnection.setConnectTimeout(2000);
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(false);

                urlConnection.connect();
                String response = null;
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    response = NetworkTools.convertStreamToString(in);
                }
                finally {
                    urlConnection.disconnect();
                }
                if(response != null) {
                    //parse json
                    JSONObject json = new JSONObject(response);
                    if(json.has("data")) {
                        JSONArray jsData = json.getJSONArray("data");
                        if(jsData != null && !jsData.isNull(0)) {
                            JSONObject userJS = jsData.getJSONObject(0);
                            String receivedUserName = userJS.getString("username");
                            if(receivedUserName.equalsIgnoreCase(userName)) {
                                userId = userJS.getString("id");
                            }
                            else {
                                mErrorMessage = getString(R.string.error_msg_user_not_found);
                            }
                        }
                        else {
                            mErrorMessage = getString(R.string.error_msg_user_not_found);
                        }
                    }
                }
            }
            catch(JSONException e) {
                e.printStackTrace();
            }
            catch(MalformedURLException e) {
                e.printStackTrace();
            }
            catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            catch(ProtocolException e) {
                e.printStackTrace();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(NullPointerException e) {
                //android bug: http://code.google.com/p/android/issues/detail?id=16895
                e.printStackTrace();
            }
            return userId;
        }
    }
}

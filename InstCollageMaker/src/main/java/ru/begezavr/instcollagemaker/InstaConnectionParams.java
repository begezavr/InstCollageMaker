package ru.begezavr.instcollagemaker;


public class InstaConnectionParams {
    public static final String CLIENT_ID = "f4aa838fe1264cd490f96f8a603df5d4";
    public static final String CLIENT_SECRET = "d746868a11034415a4b3f3d179737bf9";
    //Used for Authentication.
    public static final String AUTH_URL = "https://api.instagram.com/oauth/authorize/";
    //Used for getting token and User details.
    public static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    //Used to specify the API version which we are going to use.
    public static final String API_URL = "https://api.instagram.com/v1";
    //The callback url that we have used while registering the application.
    public static String CALLBACK_URL = "http://begezavr.ru";
}

package ru.begezavr.instcollagemaker.service;

import java.util.ArrayList;

import ru.begezavr.instcollagemaker.photo.InstaPhoto;

public interface InstaServiceClientCallbackInterface {
    void onPhotosLoaded(ArrayList<InstaPhoto> photos);
    void onPhotosLoadError(String message);
}

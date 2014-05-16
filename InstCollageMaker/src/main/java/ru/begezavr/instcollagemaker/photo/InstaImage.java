package ru.begezavr.instcollagemaker.photo;

import java.io.Serializable;

public class InstaImage implements Serializable {
    private static final long serialVersionUID = 1L;
    public InstaImage(String u, int w, int h) {
        url = u;
        width = w;
        height = h;
    }
    public String url;
    public int width;
    public int height;
}

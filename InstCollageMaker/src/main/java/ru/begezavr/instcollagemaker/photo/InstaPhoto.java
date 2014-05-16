package ru.begezavr.instcollagemaker.photo;

import java.io.Serializable;

public class InstaPhoto implements Serializable {
    private static final long serialVersionUID = 1L;
    public InstaImage low;
    public InstaImage thumb;
    public InstaImage standard;
    public int likes;
}

package ru.begezavr.instcollagemaker.photo;

import java.util.Comparator;

public class InstaPhotoComparatorByLikes implements Comparator<InstaPhoto> {
    @Override
    public int compare(InstaPhoto p1, InstaPhoto p2) {
        return p1.likes < p2.likes ? -1 : (p1.likes == p2.likes ? 0 : 1);
    }
}

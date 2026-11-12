package com.musicapp.browse;

import java.util.Locale;

public enum FolderType {
    DECADES,
    ARTISTS,
    MOVIES;

    public static FolderType parse(String raw) {
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown folder type '" + raw + "' (use decades, artists or movies)");
        }
    }
}

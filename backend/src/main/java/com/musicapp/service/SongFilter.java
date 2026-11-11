package com.musicapp.service;

/** Optional filters for listing songs; null / false means "no filter". */
/** @param decade "latest" or a decade start year such as "1990" */
public record SongFilter(String query, boolean downloadedOnly, String decade, String artist, String movie) {

    public static SongFilter none() {
        return new SongFilter(null, false, null, null, null);
    }

    public SongFilter {
        query = blankToNull(query);
        decade = blankToNull(decade);
        artist = blankToNull(artist);
        movie = blankToNull(movie);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}

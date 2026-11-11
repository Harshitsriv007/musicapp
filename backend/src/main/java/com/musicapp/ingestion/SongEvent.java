package com.musicapp.ingestion;

import java.time.LocalDate;

/** Message published to Kafka for every song discovered by a {@link com.musicapp.ingestion.provider.MusicProvider}. */
public record SongEvent(
        String source,
        String externalId,
        String title,
        String artist,
        String album,
        String genre,
        String imageUrl,
        String streamUrl,
        Integer durationSeconds,
        LocalDate releaseDate) {

    /** Kafka record key: keeps every update of the same song on the same partition (ordered). */
    public String key() {
        return source + ":" + externalId;
    }

    public void validate() {
        if (isBlank(source) || isBlank(externalId) || isBlank(title) || isBlank(streamUrl)) {
            throw new InvalidSongEventException("source, externalId, title and streamUrl are required: " + this);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

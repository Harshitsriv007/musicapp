package com.musicapp.web.dto;

import com.musicapp.domain.Song;
import java.time.Instant;
import java.time.LocalDate;

public record SongDto(
        Long id,
        String title,
        String artist,
        String album,
        String genre,
        String imageUrl,
        String streamUrl,
        Integer durationSeconds,
        LocalDate releaseDate,
        String source,
        Instant addedAt,
        boolean liked) {

    public static SongDto from(Song s, boolean liked) {
        return new SongDto(s.getId(), s.getTitle(), s.getArtist(), s.getAlbum(), s.getGenre(),
                s.getImageUrl(), s.getStreamUrl(), s.getDurationSeconds(), s.getReleaseDate(),
                s.getSource(), s.getCreatedAt(), liked);
    }
}

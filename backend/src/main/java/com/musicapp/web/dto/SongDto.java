package com.musicapp.web.dto;

import com.musicapp.domain.DownloadStatus;
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
        boolean liked,
        String downloadStatus) {

    /** Downloaded songs are streamed from our own database instead of the external API. */
    public static String localAudioPath(Long songId) {
        return "/api/songs/" + songId + "/audio";
    }

    public static SongDto from(Song s, boolean liked) {
        boolean local = s.getDownloadStatus() == DownloadStatus.DOWNLOADED;
        return new SongDto(s.getId(), s.getTitle(), s.getArtist(), s.getAlbum(), s.getGenre(),
                s.getImageUrl(), local ? localAudioPath(s.getId()) : s.getStreamUrl(),
                s.getDurationSeconds(), s.getReleaseDate(), s.getSource(), s.getCreatedAt(), liked,
                s.getDownloadStatus().name());
    }
}

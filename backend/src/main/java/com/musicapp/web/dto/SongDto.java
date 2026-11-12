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
        String movie,
        String genre,
        String imageUrl,
        String streamUrl,
        Integer durationSeconds,
        LocalDate releaseDate,
        String source,
        Instant addedAt,
        boolean liked,
        String downloadStatus,
        /** When the song first came out (MusicBrainz), if known; releaseDate is the upload date. */
        LocalDate originalReleaseDate) {

    /** Downloaded songs are streamed from our own database instead of the external API. */
    public static String localAudioPath(Long songId) {
        return "/api/songs/" + songId + "/audio";
    }

    /**
     * @param liked      the song is in this listener's liked songs
     * @param downloaded the song is in this listener's Downloads; otherwise its status is shown as NONE,
     *                   whatever other listeners did with it
     */
    public static SongDto from(Song s, boolean liked, boolean downloaded) {
        DownloadStatus status = downloaded ? s.getDownloadStatus() : DownloadStatus.NONE;
        boolean local = status == DownloadStatus.DOWNLOADED;
        return new SongDto(s.getId(), s.getTitle(), s.getArtist(), s.getAlbum(), s.getMovie(), s.getGenre(),
                s.getImageUrl(), local ? localAudioPath(s.getId()) : s.getStreamUrl(),
                s.getDurationSeconds(), s.getReleaseDate(), s.getSource(), s.getCreatedAt(), liked,
                status.name(), s.getOriginalReleaseDate());
    }
}

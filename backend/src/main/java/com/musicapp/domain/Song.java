package com.musicapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "songs", uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}))
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 512)
    private String title;

    private String artist;
    private String album;
    private String genre;

    /** Movie (or album) the song belongs to – see MovieExtractor. */
    private String movie;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "stream_url", nullable = false, length = 1024)
    private String streamUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "share_url", length = 1024)
    private String shareUrl;

    @Column(name = "license_url", length = 1024)
    private String licenseUrl;

    /** False when the provider forbids storing the audio (Jamendo's audiodownload_allowed). */
    @Column(name = "download_allowed", nullable = false)
    private boolean downloadAllowed = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_status", nullable = false, length = 16)
    private DownloadStatus downloadStatus = DownloadStatus.NONE;

    @Column(name = "audio_content_type", length = 64)
    private String audioContentType;

    @Column(name = "audio_size")
    private Long audioSize;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(name = "download_error", length = 512)
    private String downloadError;

    /** Original release (MusicBrainz); release_date is when it was uploaded. */
    @Column(name = "original_release_date")
    private LocalDate originalReleaseDate;

    @Column(name = "metadata_checked_at")
    private Instant metadataCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Song() {}

    public Song(String source, String externalId) {
        this.source = source;
        this.externalId = externalId;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getGenre() { return genre; }
    public String getMovie() { return movie; }
    public void setMovie(String movie) { this.movie = movie; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Instant getCreatedAt() { return createdAt; }
    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public boolean isDownloadAllowed() { return downloadAllowed; }
    public void setDownloadAllowed(boolean downloadAllowed) { this.downloadAllowed = downloadAllowed; }
    public LocalDate getOriginalReleaseDate() { return originalReleaseDate; }
    public void setOriginalReleaseDate(LocalDate originalReleaseDate) { this.originalReleaseDate = originalReleaseDate; }
    public Instant getMetadataCheckedAt() { return metadataCheckedAt; }
    public void setMetadataCheckedAt(Instant metadataCheckedAt) { this.metadataCheckedAt = metadataCheckedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public DownloadStatus getDownloadStatus() { return downloadStatus; }
    public String getAudioContentType() { return audioContentType; }
    public Long getAudioSize() { return audioSize; }
    public Instant getDownloadedAt() { return downloadedAt; }
    public String getDownloadError() { return downloadError; }

    public void markPending() {
        downloadStatus = DownloadStatus.PENDING;
        downloadError = null;
    }

    public void markDownloading() {
        downloadStatus = DownloadStatus.DOWNLOADING;
    }

    public void markDownloaded(String contentType, long size, Instant at) {
        downloadStatus = DownloadStatus.DOWNLOADED;
        audioContentType = contentType;
        audioSize = size;
        downloadedAt = at;
        downloadError = null;
    }

    public void markFailed(String error) {
        downloadStatus = DownloadStatus.FAILED;
        audioContentType = null;
        audioSize = null;
        downloadedAt = null;
        downloadError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 512));
    }

    public void clearDownload() {
        downloadStatus = DownloadStatus.NONE;
        audioContentType = null;
        audioSize = null;
        downloadedAt = null;
        downloadError = null;
    }

    /** Test helper: entities are normally given ids by the database. */
    public Song withId(Long id) {
        this.id = id;
        return this;
    }
}

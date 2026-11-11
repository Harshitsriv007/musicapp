package com.musicapp.domain;

/** Lifecycle of a song's audio being copied into our own database. */
public enum DownloadStatus {
    NONE,
    PENDING,
    DOWNLOADING,
    DOWNLOADED,
    FAILED;

    /** True when a new download request should be queued. */
    public boolean canRequest() {
        return this == NONE || this == FAILED;
    }
}

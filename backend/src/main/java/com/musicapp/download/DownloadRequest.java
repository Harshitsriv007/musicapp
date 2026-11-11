package com.musicapp.download;

/** Kafka message asking the downloader to copy a song's audio into our database. */
public record DownloadRequest(Long songId) {}

package com.musicapp.download;

/** Published when a listener starts a song; used to cache audio on first play. */
public record SongPlayedEvent(Long songId) {}

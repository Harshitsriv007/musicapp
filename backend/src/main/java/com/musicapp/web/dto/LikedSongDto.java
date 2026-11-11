package com.musicapp.web.dto;

import java.time.Instant;

public record LikedSongDto(SongDto song, Instant likedAt) {}

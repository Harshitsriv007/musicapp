package com.musicapp.web.dto;

import java.time.Instant;

public record HistoryEntryDto(Long id, SongDto song, Instant playedAt) {}

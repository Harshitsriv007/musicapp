package com.musicapp.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayRequest(@NotNull @Positive Long songId) {}

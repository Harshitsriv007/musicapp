package com.musicapp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(@NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 128) String password) {}

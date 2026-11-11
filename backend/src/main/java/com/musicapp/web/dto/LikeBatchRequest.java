package com.musicapp.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Like (liked=true) or unlike (liked=false) many songs at once, e.g. a whole folder. */
public record LikeBatchRequest(@NotEmpty @Size(max = 1000) List<@NotNull Long> songIds, boolean liked) {}

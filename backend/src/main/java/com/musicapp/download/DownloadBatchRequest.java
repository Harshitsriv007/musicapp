package com.musicapp.download;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DownloadBatchRequest(@NotEmpty @Size(max = 200) List<@NotNull Long> songIds) {}

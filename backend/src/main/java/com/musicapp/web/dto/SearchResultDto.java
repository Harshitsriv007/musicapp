package com.musicapp.web.dto;

import com.musicapp.browse.FolderDto;
import java.util.List;

/** Search results grouped like the home screen: matching songs, artist folders and movie folders. */
public record SearchResultDto(String query, List<SongDto> songs, List<FolderDto> artists, List<FolderDto> movies) {}

package com.musicapp.browse;

import com.musicapp.repository.SongRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowseService {

    private final SongRepository songs;

    public BrowseService(SongRepository songs) {
        this.songs = songs;
    }

    @Transactional(readOnly = true)
    public List<FolderDto> folders(FolderType type, int limit) {
        var page = PageRequest.of(0, limit);
        List<Object[]> rows = switch (type) {
            case YEARS -> songs.yearFolders(page);
            case ARTISTS -> songs.artistFolders(page);
            case MOVIES -> songs.movieFolders(page);
        };
        return rows.stream().map(r -> {
            String key = String.valueOf(r[0]);
            return new FolderDto(key, key, ((Number) r[1]).longValue(), (String) r[2]);
        }).toList();
    }
}

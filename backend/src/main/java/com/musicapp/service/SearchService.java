package com.musicapp.service;

import com.musicapp.browse.FolderDto;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.SearchResultDto;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One search box for everything: songs (by name, artist or movie), artist folders and movie folders. */
@Service
public class SearchService {

    private static final int FOLDER_LIMIT = 12;

    private final SongRepository songs;
    private final SongService songService;

    public SearchService(SongRepository songs, SongService songService) {
        this.songs = songs;
        this.songService = songService;
    }

    @Transactional(readOnly = true)
    public SearchResultDto search(String userId, String query, int songLimit) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return new SearchResultDto(query, List.of(), List.of(), List.of());
        }
        String escaped = escapeLike(q);
        String pattern = "%" + escaped + "%";
        String prefix = escaped + "%";
        var folders = PageRequest.of(0, FOLDER_LIMIT);
        return new SearchResultDto(
                query.strip(),
                songService.toDtos(userId, songs.search(pattern, prefix, PageRequest.of(0, songLimit))),
                toFolders(songs.searchArtistFolders(pattern, folders)),
                toFolders(songs.searchMovieFolders(pattern, folders)));
    }

    /** "100%_hits" must match literally, not as LIKE wildcards ("!" is the escape character in the queries). */
    static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static List<FolderDto> toFolders(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new FolderDto((String) r[0], (String) r[0], ((Number) r[1]).longValue(), (String) r[2]))
                .toList();
    }
}

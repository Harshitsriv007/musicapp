package com.musicapp.browse;

import com.musicapp.repository.SongRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowseService {

    private final SongRepository songs;
    private final Clock clock;

    public BrowseService(SongRepository songs, Clock clock) {
        this.songs = songs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FolderDto> folders(FolderType type, int limit) {
        if (type == FolderType.DECADES) {
            return decadeFolders();
        }
        var page = PageRequest.of(0, limit);
        List<Object[]> rows = type == FolderType.ARTISTS ? songs.artistFolders(page) : songs.movieFolders(page);
        return rows.stream().map(r -> {
            String key = String.valueOf(r[0]);
            return new FolderDto(key, key, ((Number) r[1]).longValue(), (String) r[2]);
        }).toList();
    }

    /** Latest, then every decade newest first – always all of them, even when empty. */
    private List<FolderDto> decadeFolders() {
        LocalDate today = LocalDate.now(clock);
        LocalDate latestFrom = Decades.range(Decades.LATEST, today).from();

        Map<Integer, long[]> counts = new LinkedHashMap<>();
        Map<Integer, String> covers = new LinkedHashMap<>();
        // yearFolders is newest year first, so each decade's cover comes from its newest year
        for (Object[] row : songs.yearFolders(PageRequest.of(0, 1000))) {
            int decade = Decades.decadeOf(((Number) row[0]).intValue());
            counts.computeIfAbsent(decade, d -> new long[1])[0] += ((Number) row[1]).longValue();
            if (row[2] != null) {
                covers.putIfAbsent(decade, (String) row[2]);
            }
        }

        List<FolderDto> folders = new ArrayList<>();
        folders.add(new FolderDto(Decades.LATEST, "Latest", songs.countReleasedSince(latestFrom),
                songs.imagesReleasedSince(latestFrom, PageRequest.of(0, 1)).stream().findFirst().orElse(null)));
        for (int decade : Decades.all(today)) {
            long count = counts.containsKey(decade) ? counts.get(decade)[0] : 0;
            folders.add(new FolderDto(String.valueOf(decade), Decades.label(decade), count, covers.get(decade)));
        }
        return folders;
    }
}

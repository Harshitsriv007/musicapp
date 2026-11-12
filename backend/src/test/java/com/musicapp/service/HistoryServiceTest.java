package com.musicapp.service;

import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.domain.PlayHistory;
import com.musicapp.domain.Song;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserDownloadRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class HistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final PlayHistoryRepository history = mock(PlayHistoryRepository.class);
    private final SongRepository songs = mock(SongRepository.class);
    private final LikedSongRepository likes = mock(LikedSongRepository.class);
    private final UserDownloadRepository downloads = mock(UserDownloadRepository.class);
    private final HistoryService service = new HistoryService(history, songs,
            new SongService(songs, likes, downloads, Clock.fixed(NOW, ZoneOffset.UTC)), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void recordPlayStoresEntryWithCurrentTime() {
        when(songs.findById(1L)).thenReturn(Optional.of(song(1, "Kesariya")));
        when(history.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var entry = service.recordPlay("riya", 1L);

        assertThat(entry.playedAt()).isEqualTo(NOW);
        assertThat(entry.song().title()).isEqualTo("Kesariya");
    }

    @Test
    void recordPlayOfUnknownSongFails() {
        when(songs.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordPlay("riya", 1L)).isInstanceOf(SongNotFoundException.class);
    }

    @Test
    void listIncludesLikedFlag() {
        when(history.findByUser(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new PlayHistory("riya", song(1, "A"), NOW), new PlayHistory("riya", song(2, "B"), NOW))));
        when(likes.findLikedSongIds(any(), anyCollection())).thenReturn(Set.of(1L));

        var page = service.list("riya", 0, 10);

        assertThat(page.content()).extracting(e -> e.song().liked()).containsExactly(true, false);
    }

    @Test
    void listShowsOnlyThisListenersDownloads() {
        Song a = song(1, "A");
        Song b = song(2, "B");
        a.markDownloaded("audio/mpeg", 3, NOW);
        b.markDownloaded("audio/mpeg", 3, NOW);
        when(history.findByUser(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new PlayHistory("riya", a, NOW), new PlayHistory("riya", b, NOW))));
        when(downloads.findDownloadedSongIds(any(), anyCollection())).thenReturn(Set.of(1L));

        var page = service.list("riya", 0, 10);

        // B is stored because someone else downloaded it, but riya did not.
        assertThat(page.content()).extracting(e -> e.song().downloadStatus()).containsExactly("DOWNLOADED", "NONE");
        assertThat(page.content().get(1).song().streamUrl()).isEqualTo("https://stream/2");
    }

    @Test
    void clearDeletesUsersHistory() {
        service.clear("riya");

        verify(history).deleteByUser("riya");
    }
}

package com.musicapp.service;

import static com.musicapp.TestFixtures.event;
import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.domain.DownloadStatus;
import com.musicapp.domain.Song;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.SongDto;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class SongServiceTest {

    private final SongRepository songs = mock(SongRepository.class);
    private final LikedSongRepository likes = mock(LikedSongRepository.class);
    private final SongService service = new SongService(songs, likes,
            java.time.Clock.fixed(java.time.Instant.parse("2026-06-15T00:00:00Z"), java.time.ZoneOffset.UTC));

    @Test
    void upsertCreatesUnknownSong() {
        when(songs.findBySourceAndExternalId("audius", "1")).thenReturn(Optional.empty());
        when(songs.saveAndFlush(any())).thenAnswer(inv -> ((Song) inv.getArgument(0)).withId(10L));

        Optional<SongDto> created = service.upsert(event("1", "Tum Hi Ho"));

        assertThat(created).get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.title()).isEqualTo("Tum Hi Ho");
            assertThat(dto.liked()).isFalse();
        });
    }

    @Test
    void upsertWorksOutTheMovieFromTheTitle() {
        when(songs.findBySourceAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(songs.saveAndFlush(any())).thenAnswer(inv -> ((Song) inv.getArgument(0)).withId(3L));

        var created = service.upsert(event("9", "Kesariya (From \"Brahmastra\")"));

        assertThat(created).get().extracting(SongDto::movie).isEqualTo("Brahmastra");
    }

    @Test
    void upsertRefreshesExistingSongWithoutReportingItAsNew() {
        Song existing = song(5, "Old title");
        when(songs.findBySourceAndExternalId("audius", "1")).thenReturn(Optional.of(existing));
        when(songs.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<SongDto> created = service.upsert(event("1", "New title"));

        assertThat(created).isEmpty();
        ArgumentCaptor<Song> saved = ArgumentCaptor.forClass(Song.class);
        verify(songs).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("New title");
    }

    @Test
    void upsertTreatsUniqueKeyRaceAsExisting() {
        when(songs.findBySourceAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(songs.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThat(service.upsert(event("1", "X"))).isEmpty();
    }

    @Test
    void downloadedSongsAreStreamedFromOurOwnApi() {
        Song s = song(7, "Kesariya");
        s.markDownloaded("audio/mpeg", 100, java.time.Instant.now());

        SongDto dto = SongDto.from(s, false);

        assertThat(dto.streamUrl()).isEqualTo("/api/songs/7/audio");
        assertThat(dto.downloadStatus()).isEqualTo("DOWNLOADED");
        assertThat(SongDto.from(song(8, "X"), false).streamUrl()).isEqualTo("https://stream/8");
    }

    @Test
    void newestMarksLikedSongsForTheUser() {
        when(songs.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(song(1, "A"), song(2, "B"))));
        when(likes.findLikedSongIds(eq("riya"), anyCollection())).thenReturn(Set.of(2L));

        List<SongDto> result = service.newest("riya", 10);

        assertThat(result).extracting(SongDto::title, SongDto::liked)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("A", false), org.assertj.core.groups.Tuple.tuple("B", true));
    }

    @Test
    void listUsesSearchWhenQueryGiven() {
        when(songs.findFiltered(eq("arijit"), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(song(1, "A"))));
        when(likes.findLikedSongIds(any(), anyCollection())).thenReturn(Set.of());

        var page = service.list("riya", new SongFilter("  arijit ", false, null, null, null), 0, 10);

        assertThat(page.content()).hasSize(1);
        verify(songs).findFiltered(eq("arijit"), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void listFiltersByFolder() {
        when(songs.findFiltered(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.list("riya", new SongFilter(null, false, "1990", " Arijit Singh ", "Brahmastra"), 0, 10);

        verify(songs).findFiltered(isNull(), isNull(), eq(java.time.LocalDate.of(1990, 1, 1)),
                eq(java.time.LocalDate.of(1999, 12, 31)), eq("Arijit Singh"), eq("Brahmastra"), any());
    }

    @Test
    void latestFolderIsTheLastTwelveMonths() {
        when(songs.findFiltered(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.list("riya", new SongFilter(null, false, "latest", null, null), 0, 10);

        verify(songs).findFiltered(isNull(), isNull(), eq(java.time.LocalDate.of(2025, 6, 15)), isNull(), isNull(), isNull(), any());
    }

    @Test
    void listCanBeRestrictedToDownloadedSongs() {
        when(songs.findFiltered(isNull(), eq(DownloadStatus.DOWNLOADED), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list("riya", new SongFilter(" ", true, null, null, null), 0, 10);

        verify(songs).findFiltered(isNull(), eq(DownloadStatus.DOWNLOADED), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void getThrowsForUnknownSong() {
        when(songs.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("riya", 99L)).isInstanceOf(SongNotFoundException.class);
    }
}

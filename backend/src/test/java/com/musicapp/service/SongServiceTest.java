package com.musicapp.service;

import static com.musicapp.TestFixtures.event;
import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private final SongService service = new SongService(songs, likes);

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
    void newestMarksLikedSongsForTheUser() {
        when(songs.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(song(1, "A"), song(2, "B"))));
        when(likes.findLikedSongIds(eq("riya"), anyCollection())).thenReturn(Set.of(2L));

        List<SongDto> result = service.newest("riya", 10);

        assertThat(result).extracting(SongDto::title, SongDto::liked)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("A", false), org.assertj.core.groups.Tuple.tuple("B", true));
    }

    @Test
    void listUsesSearchWhenQueryGiven() {
        when(songs.search(eq("arijit"), any())).thenReturn(new PageImpl<>(List.of(song(1, "A"))));
        when(likes.findLikedSongIds(any(), anyCollection())).thenReturn(Set.of());

        var page = service.list("riya", "  arijit ", 0, 10);

        assertThat(page.content()).hasSize(1);
        verify(songs).search(eq("arijit"), any());
    }

    @Test
    void getThrowsForUnknownSong() {
        when(songs.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("riya", 99L)).isInstanceOf(SongNotFoundException.class);
    }
}

package com.musicapp.service;

import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.domain.LikedSong;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class LikeServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final LikedSongRepository likes = mock(LikedSongRepository.class);
    private final SongRepository songs = mock(SongRepository.class);
    private final LikeService service = new LikeService(likes, songs, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void likeStoresSongForUser() {
        when(songs.findById(1L)).thenReturn(Optional.of(song(1, "Kesariya")));

        service.like("riya", 1L);

        ArgumentCaptor<LikedSong> saved = ArgumentCaptor.forClass(LikedSong.class);
        verify(likes).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("riya");
        assertThat(saved.getValue().getLikedAt()).isEqualTo(NOW);
    }

    @Test
    void likeIsIdempotent() {
        when(songs.findById(1L)).thenReturn(Optional.of(song(1, "Kesariya")));
        when(likes.existsByUserIdAndSongId("riya", 1L)).thenReturn(true);

        service.like("riya", 1L);

        verify(likes, never()).saveAndFlush(any());
    }

    @Test
    void likeUnknownSongFails() {
        when(songs.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.like("riya", 1L)).isInstanceOf(SongNotFoundException.class);
    }

    @Test
    void unlikeDeletesOnlyThatUsersLike() {
        service.unlike("riya", 1L);

        verify(likes).deleteByUserAndSong("riya", 1L);
    }

    @Test
    void likeAFolderSavesOnlySongsNotLikedYet() {
        when(likes.findLikedSongIds("riya", java.util.Set.of(1L, 2L, 3L))).thenReturn(java.util.Set.of(2L));
        when(songs.findAllById(any())).thenReturn(List.of(song(1, "A"), song(2, "B"), song(3, "C")));

        service.setLiked("riya", List.of(1L, 2L, 3L, 1L), true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LikedSong>> saved = ArgumentCaptor.forClass(List.class);
        verify(likes).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(l -> l.getSong().getTitle()).containsExactly("A", "C");
    }

    @Test
    void unlikeAFolderDeletesAllItsLikes() {
        service.setLiked("riya", List.of(1L, 2L), false);

        verify(likes).deleteByUserAndSongs("riya", java.util.Set.of(1L, 2L));
        verify(likes, never()).saveAll(any());
    }

    @Test
    void listMarksEverySongAsLiked() {
        when(likes.findByUser(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new LikedSong("riya", song(1, "Kesariya"), NOW))));

        var page = service.list("riya", 0, 10);

        assertThat(page.content()).singleElement().satisfies(l -> {
            assertThat(l.song().title()).isEqualTo("Kesariya");
            assertThat(l.song().liked()).isTrue();
            assertThat(l.likedAt()).isEqualTo(NOW);
        });
    }
}

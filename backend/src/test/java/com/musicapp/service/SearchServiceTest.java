package com.musicapp.service;

import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.musicapp.browse.FolderDto;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

    private final SongRepository songs = mock(SongRepository.class);
    private final LikedSongRepository likes = mock(LikedSongRepository.class);
    private final SearchService service = new SearchService(songs, new SongService(songs, likes, Clock.systemUTC()));

    @Test
    void searchesSongsArtistsAndMoviesCaseInsensitively() {
        when(songs.search(eq("%arijit%"), eq("arijit%"), any())).thenReturn(List.of(song(1, "Tum Hi Ho")));
        when(songs.searchArtistFolders(eq("%arijit%"), any())).thenReturn(List.<Object[]>of(new Object[] {"Arijit Singh", 3L, "img"}));
        when(songs.searchMovieFolders(eq("%arijit%"), any())).thenReturn(List.of());
        when(likes.findLikedSongIds(eq("riya"), anyCollection())).thenReturn(Set.of(1L));

        var result = service.search("riya", "  ARIJIT ", 20);

        assertThat(result.query()).isEqualTo("ARIJIT");
        assertThat(result.songs()).extracting(SongDto::title, SongDto::liked)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Tum Hi Ho", true));
        assertThat(result.artists()).containsExactly(new FolderDto("Arijit Singh", "Arijit Singh", 3, "img"));
        assertThat(result.movies()).isEmpty();
    }

    @Test
    void blankQueryReturnsNothingWithoutQuerying() {
        var result = service.search("riya", "   ", 20);

        assertThat(result.songs()).isEmpty();
        assertThat(result.artists()).isEmpty();
        verifyNoInteractions(songs);
    }

    @Test
    void likeWildcardsAreMatchedLiterally() {
        assertThat(SearchService.escapeLike("100%_hits!")).isEqualTo("100!%!_hits!!");
    }
}

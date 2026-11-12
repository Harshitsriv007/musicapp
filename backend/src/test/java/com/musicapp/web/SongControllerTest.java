package com.musicapp.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountService;
import com.musicapp.account.Session;
import com.musicapp.service.SongFilter;
import com.musicapp.service.SongNotFoundException;
import com.musicapp.service.SongService;
import com.musicapp.service.SongUpdatesBroadcaster;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SongController.class)
class SongControllerTest {

    private static final SongDto KESARIYA =
            new SongDto(1L, "Kesariya", "Arijit Singh", null, null, "Bollywood", "img", "stream", 268, null, "audius", null, true, "NONE", null);

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver; unknown tokens by default. */
    @MockitoBean
    AccountService accountService;

    @MockitoBean
    SongService songService;

    @MockitoBean
    SongUpdatesBroadcaster broadcaster;

    @Test
    void newSongsArePersonalisedForTheLoggedInAccount() throws Exception {
        when(accountService.authenticate("tok"))
                .thenReturn(Optional.of(new Session("tok", 7, "riya", Instant.parse("2026-01-01T04:00:00Z"))));
        when(songService.newest("acct_7", 50)).thenReturn(List.of(KESARIYA));

        mvc.perform(get("/api/songs/new").header("Authorization", "Bearer tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Kesariya"))
                .andExpect(jsonPath("$[0].liked").value(true));
    }

    @Test
    void catalogueIsPublicButNotPersonalisedWithoutALogin() throws Exception {
        when(songService.list(isNull(), eq(SongFilter.none()), eq(0), eq(30))).thenReturn(new PageResponse<>(List.of(KESARIYA), 0, 30, 1, 1));

        mvc.perform(get("/api/songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(songService).list(null, SongFilter.none(), 0, 30);
    }

    @Test
    void passesFolderFiltersToTheService() throws Exception {
        when(songService.list(isNull(), eq(new SongFilter(null, false, "1990", "Arijit Singh", "Brahmastra")), eq(0), eq(100)))
                .thenReturn(new PageResponse<>(List.of(KESARIYA), 0, 100, 1, 1));

        mvc.perform(get("/api/songs").param("decade", "1990").param("artist", "Arijit Singh")
                        .param("movie", "Brahmastra").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Kesariya"));
    }

    @Test
    void deviceIdsNoLongerSelectAnyonesData() throws Exception {
        mvc.perform(get("/api/songs/new").header("X-User-Id", "acct_7")).andExpect(status().isOk());
        verify(songService).newest(null, 50);
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/songs/new").header("Authorization", "Bearer old"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        mvc.perform(get("/api/songs").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSongIs404() throws Exception {
        when(songService.get(null, 9L)).thenThrow(new SongNotFoundException(9L));

        mvc.perform(get("/api/songs/9")).andExpect(status().isNotFound());
    }
}

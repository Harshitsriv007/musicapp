package com.musicapp.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountService;
import com.musicapp.account.Session;
import com.musicapp.service.HistoryService;
import com.musicapp.service.LikeService;
import com.musicapp.web.dto.HistoryEntryDto;
import com.musicapp.web.dto.SongDto;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({LikeController.class, HistoryController.class})
class LikeAndHistoryControllerTest {

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver: "tok" is a live session of account 7. */
    @MockitoBean
    AccountService accountService;

    @BeforeEach
    void loggedIn() {
        when(accountService.authenticate("tok"))
                .thenReturn(Optional.of(new Session("tok", 7, "riya", Instant.parse("2026-01-01T04:00:00Z"))));
    }

    @MockitoBean
    LikeService likeService;

    @MockitoBean
    HistoryService historyService;

    @Test
    void likeReturnsNoContent() throws Exception {
        mvc.perform(put("/api/likes/5").header("Authorization", "Bearer tok")).andExpect(status().isNoContent());
        verify(likeService).like("acct_7", 5L);
    }

    @Test
    void unlikeReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/likes/5").header("Authorization", "Bearer tok")).andExpect(status().isNoContent());
        verify(likeService).unlike("acct_7", 5L);
    }

    @Test
    void likeWholeFolder() throws Exception {
        mvc.perform(post("/api/likes/batch").header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[1,2],\"liked\":true}"))
                .andExpect(status().isNoContent());
        verify(likeService).setLiked("acct_7", java.util.List.of(1L, 2L), true);
    }

    @Test
    void likeBatchNeedsSongs() throws Exception {
        mvc.perform(post("/api/likes/batch").header("Authorization", "Bearer tok").contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[],\"liked\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordPlayReturnsCreatedEntry() throws Exception {
        var song = new SongDto(5L, "Kesariya", null, null, null, null, null, "s", null, null, "audius", null, false, "NONE", null);
        when(historyService.recordPlay("acct_7", 5L)).thenReturn(new HistoryEntryDto(1L, song, Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(post("/api/history").header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.song.title").value("Kesariya"))
                .andExpect(jsonPath("$.playedAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void recordPlayValidatesBody() throws Exception {
        mvc.perform(post("/api/history").header("Authorization", "Bearer tok").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }

    @Test
    void clearHistory() throws Exception {
        mvc.perform(delete("/api/history").header("Authorization", "Bearer tok")).andExpect(status().isNoContent());
        verify(historyService).clear("acct_7");
    }
}

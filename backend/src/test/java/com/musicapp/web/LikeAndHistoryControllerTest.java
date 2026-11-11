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
import com.musicapp.service.HistoryService;
import com.musicapp.service.LikeService;
import com.musicapp.web.dto.HistoryEntryDto;
import com.musicapp.web.dto.SongDto;
import java.time.Instant;
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

    /** Needed by the @CurrentUser resolver; unknown tokens by default. */
    @MockitoBean
    AccountService accountService;

    @MockitoBean
    LikeService likeService;

    @MockitoBean
    HistoryService historyService;

    @Test
    void likeReturnsNoContent() throws Exception {
        mvc.perform(put("/api/likes/5").header("X-User-Id", "riya")).andExpect(status().isNoContent());
        verify(likeService).like("riya", 5L);
    }

    @Test
    void unlikeReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/likes/5").header("X-User-Id", "riya")).andExpect(status().isNoContent());
        verify(likeService).unlike("riya", 5L);
    }

    @Test
    void likeWholeFolder() throws Exception {
        mvc.perform(post("/api/likes/batch").header("X-User-Id", "riya")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[1,2],\"liked\":true}"))
                .andExpect(status().isNoContent());
        verify(likeService).setLiked("riya", java.util.List.of(1L, 2L), true);
    }

    @Test
    void likeBatchNeedsSongs() throws Exception {
        mvc.perform(post("/api/likes/batch").contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[],\"liked\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordPlayReturnsCreatedEntry() throws Exception {
        var song = new SongDto(5L, "Kesariya", null, null, null, null, null, "s", null, null, "audius", null, false, "NONE", null);
        when(historyService.recordPlay("riya", 5L)).thenReturn(new HistoryEntryDto(1L, song, Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(post("/api/history").header("X-User-Id", "riya")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.song.title").value("Kesariya"))
                .andExpect(jsonPath("$.playedAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void recordPlayValidatesBody() throws Exception {
        mvc.perform(post("/api/history").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }

    @Test
    void clearHistory() throws Exception {
        mvc.perform(delete("/api/history").header("X-User-Id", "riya")).andExpect(status().isNoContent());
        verify(historyService).clear("riya");
    }
}

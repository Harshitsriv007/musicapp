package com.musicapp.download;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountService;
import com.musicapp.account.Session;
import com.musicapp.service.SongNotFoundException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(DownloadController.class)
class DownloadControllerTest {

    private static final byte[] AUDIO = "0123456789".getBytes();

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver: "tok" is a live session of account 7. */
    @MockitoBean
    AccountService accountService;

    @MockitoBean
    DownloadService downloadService;

    @MockitoBean
    AudioStorage storage;

    @BeforeEach
    void storedAudio() throws Exception {
        when(accountService.authenticate("tok"))
                .thenReturn(Optional.of(new Session("tok", 7, "riya", Instant.parse("2026-01-01T04:00:00Z"))));
        when(downloadService.audioInfo(1L)).thenReturn(new DownloadService.AudioInfo(AUDIO.length, "audio/mpeg"));
        doAnswer(inv -> {
            long start = inv.getArgument(1);
            long end = inv.getArgument(2);
            OutputStream out = inv.getArgument(3);
            out.write(Arrays.copyOfRange(AUDIO, (int) start, (int) end + 1));
            return null;
        }).when(storage).write(eq(1L), anyLong(), anyLong(), any());
    }

    @Test
    void servesWholeFile() throws Exception {
        MvcResult async = mvc.perform(get("/api/songs/1/audio")).andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().longValue("Content-Length", 10))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(AUDIO));
    }

    @Test
    void servesRequestedRangeForSeeking() throws Exception {
        MvcResult async = mvc.perform(get("/api/songs/1/audio").header("Range", "bytes=2-5"))
                .andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(async))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-5/10"))
                .andExpect(content().bytes("2345".getBytes()));
    }

    @Test
    void unsatisfiableRangeIs416() throws Exception {
        mvc.perform(get("/api/songs/1/audio").header("Range", "bytes=50-"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */10"));
    }

    @Test
    void notDownloadedIs404() throws Exception {
        when(downloadService.audioInfo(2L)).thenThrow(new SongNotFoundException(2L));

        mvc.perform(get("/api/songs/2/audio")).andExpect(status().isNotFound());
    }

    @Test
    void downloadManyQueuesSongs() throws Exception {
        when(downloadService.request("acct_7", List.of(1L, 2L)))
                .thenReturn(List.of(new DownloadStatusDto(1L, "PENDING"), new DownloadStatusDto(2L, "DOWNLOADED")));

        mvc.perform(post("/api/downloads").header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[1,2]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].downloadStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].downloadStatus").value("DOWNLOADED"));
    }

    @Test
    void downloadManyRejectsEmptyList() throws Exception {
        mvc.perform(post("/api/downloads").header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeDownload() throws Exception {
        mvc.perform(delete("/api/songs/1/download").header("Authorization", "Bearer tok")).andExpect(status().isNoContent());
        verify(downloadService).remove("acct_7", 1L);
    }

    @Test
    void downloadsNeedALogin() throws Exception {
        mvc.perform(post("/api/songs/1/download")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/songs/1/download")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/downloads").contentType(MediaType.APPLICATION_JSON).content("{\"songIds\":[1]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(downloadService);
    }
}

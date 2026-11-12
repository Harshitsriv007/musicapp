package com.musicapp.browse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountService;
import com.musicapp.repository.SongRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrowseController.class)
@Import({BrowseService.class, BrowseControllerTest.TestClock.class})
class BrowseControllerTest {

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver; unknown tokens by default. */
    @MockitoBean
    AccountService accountService;

    @MockitoBean
    SongRepository songs;

    @Test
    void decadeFoldersAlwaysListLatestAndEveryDecadeNewestFirst() throws Exception {
        // clock (TestClock) is 2026-06-15
        when(songs.yearFolders(any(Pageable.class))).thenReturn(List.of(
                new Object[] {2025, 20L, "https://img/2025.jpg"},
                new Object[] {2021, 5L, null},
                new Object[] {1994, 2L, "https://img/1994.jpg"},
                new Object[] {1942, 1L, null}));
        when(songs.countReleasedSince(LocalDate.of(2025, 6, 15))).thenReturn(7L);
        when(songs.imagesReleasedSince(any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/browse/decades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.contains(
                        "Latest", "20s", "10s", "00s", "90s", "80s", "70s", "60s", "50s")))
                .andExpect(jsonPath("$[0].key").value("latest"))
                .andExpect(jsonPath("$[0].songCount").value(7))
                .andExpect(jsonPath("$[1].key").value("2020"))
                .andExpect(jsonPath("$[1].songCount").value(25))
                .andExpect(jsonPath("$[1].imageUrl").value("https://img/2025.jpg"))
                .andExpect(jsonPath("$[2].songCount").value(0))
                .andExpect(jsonPath("$[4].songCount").value(2))
                .andExpect(jsonPath("$[8].songCount").value(1)); // 1942 lands in the 50s folder
    }

    @Test
    void artistAndMovieFolders() throws Exception {
        when(songs.artistFolders(any(Pageable.class))).thenReturn(List.<Object[]>of(new Object[] {"Arijit Singh", 3L, null}));
        when(songs.movieFolders(eq(org.springframework.data.domain.PageRequest.of(0, 10))))
                .thenReturn(List.<Object[]>of(new Object[] {"Brahmastra", 2L, null}));

        mvc.perform(get("/api/browse/artists")).andExpect(jsonPath("$[0].key").value("Arijit Singh"));
        mvc.perform(get("/api/browse/MOVIES").param("limit", "10")).andExpect(jsonPath("$[0].songCount").value(2));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClock {
        @org.springframework.context.annotation.Bean
        java.time.Clock clock() {
            return java.time.Clock.fixed(java.time.Instant.parse("2026-06-15T00:00:00Z"), java.time.ZoneOffset.UTC);
        }
    }

    @Test
    void unknownFolderTypeIs400() throws Exception {
        mvc.perform(get("/api/browse/years")).andExpect(status().isBadRequest());
    }
}

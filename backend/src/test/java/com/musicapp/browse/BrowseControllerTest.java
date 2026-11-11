package com.musicapp.browse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.repository.SongRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrowseController.class)
@Import(BrowseService.class)
class BrowseControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SongRepository songs;

    @Test
    void yearFolders() throws Exception {
        when(songs.yearFolders(any(Pageable.class))).thenReturn(List.of(
                new Object[] {2025, 20L, "https://img/a.jpg"}, new Object[] {2024, 5L, null}));

        mvc.perform(get("/api/browse/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("2025"))
                .andExpect(jsonPath("$[0].songCount").value(20))
                .andExpect(jsonPath("$[0].imageUrl").value("https://img/a.jpg"))
                .andExpect(jsonPath("$[1].title").value("2024"));
    }

    @Test
    void artistAndMovieFolders() throws Exception {
        when(songs.artistFolders(any(Pageable.class))).thenReturn(List.<Object[]>of(new Object[] {"Arijit Singh", 3L, null}));
        when(songs.movieFolders(eq(org.springframework.data.domain.PageRequest.of(0, 10))))
                .thenReturn(List.<Object[]>of(new Object[] {"Brahmastra", 2L, null}));

        mvc.perform(get("/api/browse/artists")).andExpect(jsonPath("$[0].key").value("Arijit Singh"));
        mvc.perform(get("/api/browse/MOVIES").param("limit", "10")).andExpect(jsonPath("$[0].songCount").value(2));
    }

    @Test
    void unknownFolderTypeIs400() throws Exception {
        mvc.perform(get("/api/browse/genres")).andExpect(status().isBadRequest());
    }
}

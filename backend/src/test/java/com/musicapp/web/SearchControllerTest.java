package com.musicapp.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountService;
import com.musicapp.browse.FolderDto;
import com.musicapp.service.SearchService;
import com.musicapp.web.dto.SearchResultDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver; unknown tokens by default. */
    @MockitoBean
    AccountService accountService;

    @MockitoBean
    SearchService searchService;

    @Test
    void returnsGroupedResults() throws Exception {
        when(searchService.search("riya", "brahmastra", 50)).thenReturn(new SearchResultDto("brahmastra", List.of(),
                List.of(), List.of(new FolderDto("Brahmastra", "Brahmastra", 2, null))));

        mvc.perform(get("/api/search").param("q", "brahmastra").header("X-User-Id", "riya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("brahmastra"))
                .andExpect(jsonPath("$.movies[0].key").value("Brahmastra"))
                .andExpect(jsonPath("$.movies[0].songCount").value(2));
    }

    @Test
    void rejectsOverlongQueries() throws Exception {
        mvc.perform(get("/api/search").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
    }
}

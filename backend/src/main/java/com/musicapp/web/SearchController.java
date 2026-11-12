package com.musicapp.web;

import com.musicapp.service.SearchService;
import com.musicapp.web.dto.SearchResultDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Search songs by name, artist or movie: GET /api/search?q=arijit */
    @GetMapping("/api/search")
    public SearchResultDto search(
            @CurrentUser(required = false) String userId,
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return searchService.search(userId, q, limit);
    }
}

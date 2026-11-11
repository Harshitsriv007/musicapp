package com.musicapp.browse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Folders for the home screen: songs grouped by release year, artist and movie/album. */
@RestController
@Validated
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    /** {@code type} is years, artists or movies. Songs of a folder: GET /api/songs?year=|artist=|movie= */
    @GetMapping("/api/browse/{type}")
    public List<FolderDto> folders(@PathVariable String type,
                                   @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int limit) {
        return browseService.folders(FolderType.parse(type), limit);
    }
}

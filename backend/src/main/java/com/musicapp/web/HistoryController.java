package com.musicapp.web;

import com.musicapp.service.HistoryService;
import com.musicapp.web.dto.HistoryEntryDto;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.PlayRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@Validated
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PageResponse<HistoryEntryDto> list(
            @CurrentUser String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return historyService.list(userId, page, size);
    }

    @PostMapping
    public ResponseEntity<HistoryEntryDto> recordPlay(
            @CurrentUser String userId,
            @Valid @RequestBody PlayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historyService.recordPlay(userId, request.songId()));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(
            @CurrentUser String userId) {
        historyService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}

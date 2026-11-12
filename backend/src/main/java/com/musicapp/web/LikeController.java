package com.musicapp.web;

import com.musicapp.service.LikeService;
import com.musicapp.web.dto.LikeBatchRequest;
import com.musicapp.web.dto.LikedSongDto;
import com.musicapp.web.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
@Validated
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public PageResponse<LikedSongDto> list(
            @CurrentUser String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return likeService.list(userId, page, size);
    }

    /** Like / unlike a whole folder: {"songIds":[...], "liked": true|false}. */
    @PostMapping("/batch")
    public ResponseEntity<Void> batch(
            @CurrentUser String userId,
            @Valid @RequestBody LikeBatchRequest request) {
        likeService.setLiked(userId, request.songIds(), request.liked());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{songId}")
    public ResponseEntity<Void> like(
            @CurrentUser String userId,
            @PathVariable Long songId) {
        likeService.like(userId, songId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> unlike(
            @CurrentUser String userId,
            @PathVariable Long songId) {
        likeService.unlike(userId, songId);
        return ResponseEntity.noContent().build();
    }
}

package com.musicapp.download;

import com.musicapp.web.CurrentUser;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class DownloadController {

    private final DownloadService downloadService;
    private final AudioStorage storage;

    public DownloadController(DownloadService downloadService, AudioStorage storage) {
        this.downloadService = downloadService;
        this.storage = storage;
    }

    /** Download several songs (e.g. "Download all" on Liked songs). */
    @PostMapping("/api/downloads")
    public ResponseEntity<List<DownloadStatusDto>> downloadMany(@CurrentUser String userId,
                                                                @Valid @RequestBody DownloadBatchRequest body) {
        return ResponseEntity.accepted().body(downloadService.request(userId, body.songIds()));
    }

    @PostMapping("/api/songs/{id}/download")
    public ResponseEntity<DownloadStatusDto> download(@CurrentUser String userId, @PathVariable Long id) {
        return ResponseEntity.accepted().body(downloadService.request(userId, List.of(id)).getFirst());
    }

    @DeleteMapping("/api/songs/{id}/download")
    public ResponseEntity<Void> remove(@CurrentUser String userId, @PathVariable Long id) {
        downloadService.remove(userId, id);
        return ResponseEntity.noContent().build();
    }

    /** Plays a downloaded song from MySQL. Supports Range requests so seeking is instant. */
    @GetMapping("/api/songs/{id}/audio")
    public ResponseEntity<StreamingResponseBody> audio(@PathVariable Long id,
                                                       @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        DownloadService.AudioInfo info = downloadService.audioInfo(id);
        long size = info.size();
        Optional<ByteRange> requested;
        try {
            requested = ByteRange.parse(rangeHeader, size);
        } catch (ByteRange.NotSatisfiableException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + size)
                    .build();
        }
        ByteRange range = requested.orElse(new ByteRange(0, size - 1));
        StreamingResponseBody body = out -> storage.write(id, range.start(), range.end(), out);

        var response = ResponseEntity.status(requested.isPresent() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType(info.contentType() == null ? "audio/mpeg" : info.contentType()))
                .contentLength(range.length())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate());
        requested.ifPresent(r -> response.header(HttpHeaders.CONTENT_RANGE,
                "bytes " + r.start() + "-" + r.end() + "/" + size));
        return response.body(body);
    }
}

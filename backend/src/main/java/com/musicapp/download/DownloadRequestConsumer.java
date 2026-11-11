package com.musicapp.download;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Performs downloads asynchronously, off the HTTP request thread. */
@Component
public class DownloadRequestConsumer {

    private final DownloadService downloadService;

    public DownloadRequestConsumer(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @KafkaListener(
            topics = "${musicapp.kafka.downloads-topic}",
            groupId = "music-song-downloader",
            concurrency = "2",
            properties = "spring.json.value.default.type=com.musicapp.download.DownloadRequest")
    public void onDownloadRequest(DownloadRequest request) {
        if (request == null || request.songId() == null) {
            return;
        }
        downloadService.download(request.songId());
    }
}

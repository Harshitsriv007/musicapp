package com.musicapp.download;

import com.musicapp.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DownloadRequestProducer {

    private static final Logger log = LoggerFactory.getLogger(DownloadRequestProducer.class);

    private final KafkaTemplate<String, DownloadRequest> kafkaTemplate;
    private final String topic;

    public DownloadRequestProducer(KafkaTemplate<String, DownloadRequest> kafkaTemplate, AppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = props.kafka().downloadsTopic();
    }

    public void publish(Long songId) {
        kafkaTemplate.send(topic, String.valueOf(songId), new DownloadRequest(songId))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.warn("Could not queue download of song {}: {}", songId, ex.getMessage());
                    }
                });
    }
}

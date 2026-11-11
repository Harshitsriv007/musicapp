package com.musicapp.ingestion;

import com.musicapp.config.AppProperties;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/** Publishes song events asynchronously; callers get a future instead of blocking on the broker. */
@Component
public class SongEventProducer {

    private static final Logger log = LoggerFactory.getLogger(SongEventProducer.class);

    private final KafkaTemplate<String, SongEvent> kafkaTemplate;
    private final String topic;

    public SongEventProducer(KafkaTemplate<String, SongEvent> kafkaTemplate, AppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = props.kafka().songsTopic();
    }

    public CompletableFuture<SendResult<String, SongEvent>> publish(SongEvent event) {
        return kafkaTemplate.send(topic, event.key(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish song {}: {}", event.key(), ex.getMessage());
            } else {
                log.debug("Published song {} to {}-{}@{}", event.key(), topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}

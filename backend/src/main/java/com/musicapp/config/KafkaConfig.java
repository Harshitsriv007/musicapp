package com.musicapp.config;

import com.musicapp.ingestion.InvalidSongEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic songsTopic(AppProperties props) {
        return TopicBuilder.name(props.kafka().songsTopic())
                .partitions(props.kafka().partitions())
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic songsDeadLetterTopic(AppProperties props) {
        return TopicBuilder.name(props.kafka().deadLetterTopic())
                .partitions(props.kafka().partitions())
                .replicas(1)
                .build();
    }

    /**
     * Retries a failing record 3 times (1s apart), then parks it on the dead-letter topic so one
     * bad message never blocks the partition. Invalid payloads skip the retries.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations, AppProperties props) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        props.kafka().deadLetterTopic(), record.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
        handler.addNotRetryableExceptions(InvalidSongEventException.class);
        return handler;
    }
}

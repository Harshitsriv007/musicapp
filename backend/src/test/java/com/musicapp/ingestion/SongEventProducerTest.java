package com.musicapp.ingestion;

import static com.musicapp.TestFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.TestFixtures;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class SongEventProducerTest {

    @Test
    @SuppressWarnings("unchecked")
    void sendsToSongsTopicKeyedBySourceAndId() {
        KafkaTemplate<String, SongEvent> template = mock(KafkaTemplate.class);
        SongEvent e = event("42", "Kesariya");
        CompletableFuture<SendResult<String, SongEvent>> future = new CompletableFuture<>();
        when(template.send("music.songs.discovered", "audius:42", e)).thenReturn(future);

        var producer = new SongEventProducer(template, TestFixtures.props("https://a", List.of("q")));
        var result = producer.publish(e);

        verify(template).send("music.songs.discovered", "audius:42", e);
        assertThat(result).isNotDone(); // non-blocking: completes when the broker acks
    }
}

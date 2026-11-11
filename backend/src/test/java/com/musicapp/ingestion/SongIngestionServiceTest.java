package com.musicapp.ingestion;

import static com.musicapp.TestFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.ingestion.provider.MusicProvider;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SongIngestionServiceTest {

    private final SongEventProducer producer = mock(SongEventProducer.class);

    private static MusicProvider provider(String name, List<SongEvent> songs) {
        MusicProvider p = mock(MusicProvider.class);
        when(p.name()).thenReturn(name);
        when(p.fetchLatest()).thenReturn(songs);
        return p;
    }

    @Test
    void publishesEveryUniqueSongToKafka() {
        when(producer.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        var service = new SongIngestionService(
                List.of(provider("audius", List.of(event("1", "Tum Hi Ho"), event("2", "Kesariya"), event("1", "Tum Hi Ho")))),
                producer);

        IngestionReport report = service.ingest();

        ArgumentCaptor<SongEvent> captor = ArgumentCaptor.forClass(SongEvent.class);
        verify(producer, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(SongEvent::title).containsExactly("Tum Hi Ho", "Kesariya");
        assertThat(report).isEqualTo(new IngestionReport(false, 2, 2, List.of()));
    }

    @Test
    void failingProviderDoesNotStopOthers() {
        when(producer.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        MusicProvider broken = mock(MusicProvider.class);
        when(broken.name()).thenReturn("broken");
        when(broken.fetchLatest()).thenThrow(new IllegalStateException("down"));
        var service = new SongIngestionService(List.of(broken, provider("audius", List.of(event("1", "Kesariya")))), producer);

        IngestionReport report = service.ingest();

        assertThat(report.published()).isEqualTo(1);
        assertThat(report.failedProviders()).containsExactly("broken");
    }

    @Test
    void countsOnlySuccessfulPublishes() {
        when(producer.publish(any()))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        var service = new SongIngestionService(
                List.of(provider("audius", List.of(event("1", "A"), event("2", "B")))), producer);

        IngestionReport report = service.ingest();

        assertThat(report.fetched()).isEqualTo(2);
        assertThat(report.published()).isEqualTo(1);
    }

    @Test
    void concurrentRunIsSkipped() throws Exception {
        CountDownLatch inFetch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MusicProvider slow = mock(MusicProvider.class);
        when(slow.name()).thenReturn("slow");
        when(slow.fetchLatest()).thenAnswer(inv -> {
            inFetch.countDown();
            release.await(5, TimeUnit.SECONDS);
            return List.of();
        });
        var service = new SongIngestionService(List.of(slow), producer);

        Thread first = Thread.ofVirtual().start(service::ingest);
        assertThat(inFetch.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(service.ingest().skipped()).isTrue();

        release.countDown();
        first.join();
        assertThat(service.ingest().skipped()).isFalse();
    }
}

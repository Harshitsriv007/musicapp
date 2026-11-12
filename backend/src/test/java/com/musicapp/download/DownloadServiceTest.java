package com.musicapp.download;

import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.TestFixtures;
import com.musicapp.domain.DownloadStatus;
import com.musicapp.domain.Song;
import com.musicapp.domain.UserDownload;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserDownloadRepository;
import com.musicapp.service.SongNotFoundException;
import com.musicapp.service.SongUpdatesBroadcaster;
import com.musicapp.web.dto.SongDto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class DownloadServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final SongRepository songs = mock(SongRepository.class);
    private final UserDownloadRepository downloads = mock(UserDownloadRepository.class);
    private final AudioStorage storage = mock(AudioStorage.class);
    private final AudioFetcher fetcher = mock(AudioFetcher.class);
    private final DownloadRequestProducer producer = mock(DownloadRequestProducer.class);
    private final SongUpdatesBroadcaster broadcaster = mock(SongUpdatesBroadcaster.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private DownloadService service;
    private Song kesariya;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0))
                .doInTransaction(new SimpleTransactionStatus()));
        // TestFixtures: max 10_000 bytes per song
        service = new DownloadService(songs, downloads, storage, fetcher, producer, broadcaster, tx,
                Clock.fixed(NOW, ZoneOffset.UTC), TestFixtures.props("https://a", List.of("q")));
        kesariya = song(1, "Kesariya");
        when(songs.findById(1L)).thenReturn(Optional.of(kesariya));
        when(songs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(downloads.existsBySongId(1L)).thenReturn(true);
    }

    @Test
    void requestQueuesOnlySongsThatAreNotYetDownloaded() {
        Song done = song(2, "Done");
        done.markDownloaded("audio/mpeg", 10, NOW);
        when(songs.findAllById(List.of(1L, 2L))).thenReturn(List.of(kesariya, done));

        var statuses = service.request("acct_1", List.of(1L, 2L, 1L));

        assertThat(statuses).containsExactly(
                new DownloadStatusDto(1L, "PENDING"), new DownloadStatusDto(2L, "DOWNLOADED"));
        verify(producer).publish(1L);
        verify(producer, never()).publish(2L);
    }

    @Test
    void requestAddsSongsToThisListenersDownloadsOnly() {
        Song done = song(2, "Done");
        when(songs.findAllById(List.of(1L, 2L))).thenReturn(List.of(kesariya, done));
        when(downloads.findDownloadedSongIds(eq("acct_1"), any())).thenReturn(Set.of(2L));

        service.request("acct_1", List.of(1L, 2L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserDownload>> saved = ArgumentCaptor.forClass(List.class);
        verify(downloads).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(d -> {
            assertThat(d.getUserId()).isEqualTo("acct_1");
            assertThat(d.getSong().getId()).isEqualTo(1L);
            assertThat(d.getRequestedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void requestSkipsSongsTheProviderDoesNotLetUsStore() {
        Song locked = song(2, "Locked");
        locked.setDownloadAllowed(false);
        when(songs.findAllById(List.of(1L, 2L))).thenReturn(List.of(kesariya, locked));

        var statuses = service.request("acct_1", List.of(1L, 2L));

        assertThat(statuses).containsExactly(new DownloadStatusDto(1L, "PENDING"));
        verify(producer, never()).publish(2L);
    }

    @Test
    void requestForOnlyNonDownloadableSongsIsRejected() {
        Song locked = song(2, "Locked");
        locked.setDownloadAllowed(false);
        when(songs.findAllById(List.of(2L))).thenReturn(List.of(locked));

        assertThatThrownBy(() -> service.request("acct_1", List.of(2L))).isInstanceOf(IllegalArgumentException.class);
        verify(downloads, never()).saveAll(any());
    }

    @Test
    void requestForUnknownSongsIs404() {
        when(songs.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.request("acct_1", List.of(99L))).isInstanceOf(SongNotFoundException.class);
    }

    @Test
    void downloadStoresAudioAndAnnouncesIt() throws Exception {
        byte[] audio = new byte[] {1, 2, 3};
        when(fetcher.open("https://stream/1")).thenReturn(new FetchedAudio(new ByteArrayInputStream(audio), "audio/mpeg", 3));
        when(storage.store(eq(1L), any(), eq(10_000L))).thenReturn(3L);

        service.download(1L);

        assertThat(kesariya.getDownloadStatus()).isEqualTo(DownloadStatus.DOWNLOADED);
        assertThat(kesariya.getAudioSize()).isEqualTo(3L);
        assertThat(kesariya.getDownloadedAt()).isEqualTo(NOW);
        ArgumentCaptor<SongDto> events = ArgumentCaptor.forClass(SongDto.class);
        verify(broadcaster, org.mockito.Mockito.times(2)).songUpdated(events.capture());
        assertThat(events.getAllValues()).extracting(SongDto::downloadStatus).containsExactly("DOWNLOADING", "DOWNLOADED");
        assertThat(events.getAllValues().get(1).streamUrl()).isEqualTo("/api/songs/1/audio");
    }

    @Test
    void downloadIsSkippedWhenAlreadyStored() throws Exception {
        kesariya.markDownloaded("audio/mpeg", 3, NOW);

        service.download(1L);

        verify(fetcher, never()).open(any());
    }

    @Test
    void downloadIsSkippedWhenEveryoneRemovedItFromTheirDownloads() throws Exception {
        when(downloads.existsBySongId(1L)).thenReturn(false);

        service.download(1L);

        verify(fetcher, never()).open(any());
    }

    @Test
    void sourceFailureMarksSongFailed() throws Exception {
        when(fetcher.open(any())).thenThrow(new IOException("Audio source returned HTTP 503"));

        service.download(1L);

        assertThat(kesariya.getDownloadStatus()).isEqualTo(DownloadStatus.FAILED);
        assertThat(kesariya.getDownloadError()).contains("503");
        verify(storage).delete(1L);
    }

    @Test
    void oversizedAudioIsRejectedBeforeStoring() throws Exception {
        when(fetcher.open(any())).thenReturn(new FetchedAudio(new ByteArrayInputStream(new byte[0]), "audio/mpeg", 50_000));

        service.download(1L);

        assertThat(kesariya.getDownloadStatus()).isEqualTo(DownloadStatus.FAILED);
        verify(storage, never()).store(anyLong(), any(), anyLong());
    }

    @Test
    void removeByTheLastListenerDeletesAudioAndResetsStatus() {
        kesariya.markDownloaded("audio/mpeg", 3, NOW);
        when(downloads.existsBySongId(1L)).thenReturn(false);

        service.remove("acct_1", 1L);

        verify(downloads).deleteByUserAndSong("acct_1", 1L);
        verify(storage).delete(1L);
        assertThat(kesariya.getDownloadStatus()).isEqualTo(DownloadStatus.NONE);
    }

    @Test
    void removeKeepsTheAudioWhileAnotherListenerStillHasIt() {
        kesariya.markDownloaded("audio/mpeg", 3, NOW);

        service.remove("acct_1", 1L);

        verify(downloads).deleteByUserAndSong("acct_1", 1L);
        verify(storage, never()).delete(anyLong());
        assertThat(kesariya.getDownloadStatus()).isEqualTo(DownloadStatus.DOWNLOADED);
    }

    @Test
    void audioInfoOnlyForDownloadedSongs() {
        assertThatThrownBy(() -> service.audioInfo(1L)).isInstanceOf(SongNotFoundException.class);

        kesariya.markDownloaded("audio/mpeg", 3, NOW);

        assertThat(service.audioInfo(1L)).isEqualTo(new DownloadService.AudioInfo(3, "audio/mpeg"));
    }
}

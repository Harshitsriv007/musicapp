package com.musicapp.download;

import com.musicapp.config.AppProperties;
import com.musicapp.domain.DownloadStatus;
import com.musicapp.domain.Song;
import com.musicapp.domain.UserDownload;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserDownloadRepository;
import com.musicapp.service.SongNotFoundException;
import com.musicapp.service.SongUpdatesBroadcaster;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Copies the audio of songs the user chose to download from the external music API into
 * MySQL, so they replay from our own database: no repeated API calls and no buffering.
 * Songs that are not downloaded keep streaming live from the music API.
 *
 * <p>Each listener has their own Downloads list ({@code user_downloads}); the audio is stored once
 * per song and only deleted when the last listener removes it.
 */
@Service
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    public record AudioInfo(long size, String contentType) {}

    private final SongRepository songs;
    private final UserDownloadRepository downloads;
    private final AudioStorage storage;
    private final AudioFetcher fetcher;
    private final DownloadRequestProducer producer;
    private final SongUpdatesBroadcaster broadcaster;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final AppProperties.Download config;

    public DownloadService(SongRepository songs, UserDownloadRepository downloads, AudioStorage storage,
                           AudioFetcher fetcher, DownloadRequestProducer producer, SongUpdatesBroadcaster broadcaster,
                           TransactionTemplate tx, Clock clock, AppProperties props) {
        this.songs = songs;
        this.downloads = downloads;
        this.storage = storage;
        this.fetcher = fetcher;
        this.producer = producer;
        this.broadcaster = broadcaster;
        this.tx = tx;
        this.clock = clock;
        this.config = props.download();
    }

    /**
     * Adds the songs to this listener's Downloads and queues the ones whose audio is not stored yet.
     * Songs already stored (by anyone) are in the list straight away.
     */
    @Transactional
    public List<DownloadStatusDto> request(String userId, Collection<Long> songIds) {
        List<Long> ids = List.copyOf(new LinkedHashSet<>(songIds));
        List<Song> found = songs.findAllById(ids);
        if (found.isEmpty()) {
            throw new SongNotFoundException(ids.getFirst());
        }
        // The provider's terms forbid storing these (e.g. Jamendo tracks without audiodownload_allowed).
        found = found.stream().filter(Song::isDownloadAllowed).toList();
        if (found.isEmpty()) {
            throw new IllegalArgumentException("The music provider does not allow downloading this song");
        }
        Set<Long> already = downloads.findDownloadedSongIds(userId, found.stream().map(Song::getId).toList());
        Instant now = Instant.now(clock);
        downloads.saveAll(found.stream()
                .filter(s -> !already.contains(s.getId()))
                .map(s -> new UserDownload(userId, s, now))
                .toList());
        List<Long> queued = new ArrayList<>();
        for (Song song : found) {
            if (song.getDownloadStatus().canRequest()) {
                song.markPending();
                queued.add(song.getId());
            }
        }
        songs.saveAll(found);
        afterCommit(() -> queued.forEach(producer::publish));
        return found.stream().map(s -> new DownloadStatusDto(s.getId(), s.getDownloadStatus().name())).toList();
    }

    /** Called by the Kafka consumer. Never throws: failures are recorded on the song. */
    public void download(Long songId) {
        Song song = tx.execute(status -> {
            Song s = songs.findById(songId).orElse(null);
            // Skip when every listener removed it from their Downloads before it started.
            if (s == null || s.getDownloadStatus() == DownloadStatus.DOWNLOADED || !downloads.existsBySongId(songId)) {
                return null;
            }
            s.markDownloading();
            return songs.save(s);
        });
        if (song == null) {
            return;
        }
        broadcaster.songUpdated(SongDto.from(song, false, true));

        try (FetchedAudio audio = fetcher.open(song.getStreamUrl())) {
            if (audio.contentLength() > config.maxBytes()) {
                throw new AudioTooLargeException(config.maxBytes());
            }
            long size = storage.store(songId, audio.body(), config.maxBytes());
            finish(songId, s -> s.markDownloaded(audio.contentType(), size, Instant.now(clock)));
            log.info("Stored audio of song {} ({} bytes)", songId, size);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(songId, "interrupted");
        } catch (Exception e) {
            log.warn("Download of song {} failed: {}", songId, e.getMessage());
            fail(songId, e.getMessage());
        }
    }

    /** Removes the song from this listener's Downloads; the audio is deleted once nobody keeps it. */
    @Transactional
    public void remove(String userId, Long songId) {
        Song song = songs.findById(songId).orElseThrow(() -> new SongNotFoundException(songId));
        downloads.deleteByUserAndSong(userId, songId);
        if (downloads.existsBySongId(songId)) {
            return;
        }
        storage.delete(songId);
        song.clearDownload();
        songs.save(song);
        afterCommit(() -> broadcaster.songUpdated(SongDto.from(song, false, true)));
    }

    @Transactional(readOnly = true)
    public AudioInfo audioInfo(Long songId) {
        Song song = songs.findById(songId)
                .filter(s -> s.getDownloadStatus() == DownloadStatus.DOWNLOADED && s.getAudioSize() != null)
                .orElseThrow(() -> new SongNotFoundException(songId));
        return new AudioInfo(song.getAudioSize(), song.getAudioContentType());
    }

    private void fail(Long songId, String error) {
        storage.delete(songId);
        finish(songId, s -> s.markFailed(error));
    }

    private void finish(Long songId, java.util.function.Consumer<Song> change) {
        Song updated = tx.execute(status -> songs.findById(songId).map(s -> {
            change.accept(s);
            return songs.save(s);
        }).orElse(null));
        if (updated != null) {
            broadcaster.songUpdated(SongDto.from(updated, false, true));
        }
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}

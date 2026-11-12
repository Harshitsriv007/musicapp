package com.musicapp.enrichment;

import com.musicapp.config.AppProperties;
import com.musicapp.domain.Song;
import com.musicapp.repository.SongRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Background job: looks up the original release date of songs not checked yet, a small batch
 * at a time. Each song is checked once; songs without a confident match keep their upload date.
 */
@Service
public class ReleaseYearEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseYearEnrichmentService.class);

    public record Report(int checked, int found) {}

    private final SongRepository songs;
    private final MusicBrainzClient musicBrainz;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final AppProperties.Enrichment config;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ReleaseYearEnrichmentService(SongRepository songs, MusicBrainzClient musicBrainz, TransactionTemplate tx,
                                        Clock clock, AppProperties props) {
        this.songs = songs;
        this.musicBrainz = musicBrainz;
        this.tx = tx;
        this.clock = clock;
        this.config = props.enrichment();
    }

    @Scheduled(initialDelayString = "${musicapp.enrichment.initial-delay}", fixedDelayString = "${musicapp.enrichment.interval}")
    public void scheduledRun() {
        enrichPending();
    }

    public Report enrichPending() {
        if (!config.enabled() || !running.compareAndSet(false, true)) {
            return new Report(0, 0);
        }
        int checked = 0;
        int found = 0;
        try {
            List<Song> batch = songs.findByMetadataCheckedAtIsNullOrderByIdAsc(PageRequest.of(0, config.batchSize()));
            for (Song song : batch) {
                Optional<LocalDate> original;
                try {
                    original = musicBrainz.findOriginalReleaseDate(song.getTitle(), song.getArtist());
                } catch (Exception e) {
                    // MusicBrainz busy / offline: leave unchecked and retry on the next run.
                    log.warn("MusicBrainz lookup failed ({}), will retry later", e.getMessage());
                    break;
                }
                save(song.getId(), original.orElse(null));
                checked++;
                if (original.isPresent()) {
                    found++;
                    log.info("Original release of \"{}\": {}", song.getTitle(), original.get());
                }
            }
        } finally {
            running.set(false);
        }
        if (checked > 0) {
            log.info("Release-year lookup: checked={}, found={}", checked, found);
        }
        return new Report(checked, found);
    }

    private void save(Long songId, LocalDate original) {
        tx.executeWithoutResult(status -> songs.findById(songId).ifPresent(s -> {
            s.setOriginalReleaseDate(original);
            s.setMetadataCheckedAt(Instant.now(clock));
            songs.save(s);
        }));
    }
}

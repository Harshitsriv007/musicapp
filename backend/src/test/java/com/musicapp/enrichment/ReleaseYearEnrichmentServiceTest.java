package com.musicapp.enrichment;

import static com.musicapp.TestFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.TestFixtures;
import com.musicapp.domain.Song;
import com.musicapp.repository.SongRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ReleaseYearEnrichmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    private final SongRepository songs = mock(SongRepository.class);
    private final MusicBrainzClient musicBrainz = mock(MusicBrainzClient.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private ReleaseYearEnrichmentService service;
    private Song tumHiHo;
    private Song mashup;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        org.mockito.Mockito.doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(tx).executeWithoutResult(any());
        service = new ReleaseYearEnrichmentService(songs, musicBrainz, tx, Clock.fixed(NOW, ZoneOffset.UTC),
                TestFixtures.props("https://a", List.of("q")));
        tumHiHo = song(1, "Tum Hi Ho");
        mashup = song(2, "Lofi Mashup");
        when(songs.findByMetadataCheckedAtIsNullOrderByIdAsc(any())).thenReturn(List.of(tumHiHo, mashup));
        when(songs.findById(1L)).thenReturn(Optional.of(tumHiHo));
        when(songs.findById(2L)).thenReturn(Optional.of(mashup));
    }

    @Test
    void storesOriginalDateAndMarksEverySongChecked() {
        when(musicBrainz.findOriginalReleaseDate("Tum Hi Ho", "Arijit Singh")).thenReturn(Optional.of(LocalDate.of(2013, 3, 1)));
        when(musicBrainz.findOriginalReleaseDate("Lofi Mashup", "Arijit Singh")).thenReturn(Optional.empty());

        var report = service.enrichPending();

        assertThat(report).isEqualTo(new ReleaseYearEnrichmentService.Report(2, 1));
        assertThat(tumHiHo.getOriginalReleaseDate()).isEqualTo(LocalDate.of(2013, 3, 1));
        assertThat(tumHiHo.getMetadataCheckedAt()).isEqualTo(NOW);
        assertThat(mashup.getOriginalReleaseDate()).isNull();
        assertThat(mashup.getMetadataCheckedAt()).isEqualTo(NOW); // never looked up again
    }

    @Test
    void stopsAndRetriesLaterWhenMusicBrainzIsUnavailable() {
        when(musicBrainz.findOriginalReleaseDate(any(), any())).thenThrow(new IllegalStateException("503"));

        var report = service.enrichPending();

        assertThat(report.checked()).isZero();
        assertThat(tumHiHo.getMetadataCheckedAt()).isNull();
        verify(songs, never()).save(any());
    }
}

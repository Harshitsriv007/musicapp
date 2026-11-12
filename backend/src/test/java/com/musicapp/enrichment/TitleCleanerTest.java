package com.musicapp.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TitleCleanerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Romantic Bollywood Lofi Mashup | Chill Vibes",
            "Bombay Tu Hi Re x ATTLAS (Bollywood Mashup)",
            "Tum Hi Ho (Slowed + Reverb)",
            "Kesariya - Remix",
            "Lag Ja Gale (Cover)",
            "90s Hits Non-Stop Jukebox"})
    void remixesAndMashupsAreNotLookedUp(String title) {
        assertThat(TitleCleaner.candidates(title, "DJ")).isEmpty();
    }

    @Test
    void plainTitleUsesUploaderAndTitleAsArtistHints() {
        assertThat(TitleCleaner.candidates("Tum Hi Ho (Official Video)", "Arijit Singh"))
                .containsExactly(new TitleCleaner.Candidate("Tum Hi Ho", List.of("Arijit Singh")));
    }

    @Test
    void dashedTitleTriesBothOrders() {
        var candidates = TitleCleaner.candidates("ATIF ASLAM - RONA CHADITA_128k", "YKK");

        assertThat(candidates).extracting(TitleCleaner.Candidate::title).containsExactly("ATIF ASLAM", "RONA CHADITA");
        assertThat(candidates.get(1).artistHints()).contains("ATIF ASLAM", "YKK");
    }

    @Test
    void stripsFeaturingAndPipes() {
        assertThat(TitleCleaner.candidates("Chaleya ft. Arijit Singh | Jawan | Full Song", "SRK Fans"))
                .extracting(TitleCleaner.Candidate::title).containsExactly("Chaleya");
    }

    @Test
    void normalizeIgnoresCaseAccentsAndPunctuation() {
        assertThat(TitleCleaner.normalize("Café — Déjà-Vu!!")).isEqualTo("cafe deja vu");
        assertThat(TitleCleaner.normalize("TUM HI HO")).isEqualTo(TitleCleaner.normalize("Tum   Hi Ho"));
    }
}

package com.musicapp.ingestion.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BollywoodFilterTest {

    @Test
    void keywordsAreTheMeaningfulSearchWordsPlusBollywoodWords() {
        assertThat(BollywoodFilter.keywordsFor("Arijit Singh"))
                .containsExactlyInAnyOrder("arijit", "bollywood", "hindi", "filmi");
        assertThat(BollywoodFilter.keywordsFor("bollywood romantic songs"))
                .containsExactlyInAnyOrder("bollywood", "hindi", "filmi");
    }

    @Test
    void matchesAnyFieldIgnoringCase() {
        var keywords = BollywoodFilter.keywordsFor("atif aslam");
        assertThat(BollywoodFilter.mentionsAny(keywords, "Hona Tha Pyaar", "ATIF ASLAM", null)).isTrue();
        assertThat(BollywoodFilter.mentionsAny(keywords, "Kesariya", null, "Bollywood")).isTrue();
        assertThat(BollywoodFilter.mentionsAny(keywords, "Chill beat", "dj x", "Lo-Fi")).isFalse();
    }

    @Test
    void matchesWholeWordsOnly() {
        var keywords = BollywoodFilter.keywordsFor("atif aslam");
        assertThat(BollywoodFilter.mentionsAny(keywords, "Okan & Volkan - Halden Anlamaz", "Beatifuldead", "Pop")).isFalse();
        assertThat(BollywoodFilter.mentionsAny(keywords, "Dil_Meri_Na_Sune___Atif_Aslam___Genius")).isTrue();
        assertThat(BollywoodFilter.mentionsAny(BollywoodFilter.keywordsFor("arijit singh"),
                "Kamariya Lollypop Lagelu - Pawan Singh", "Bhojpuri Song")).isFalse();
    }
}

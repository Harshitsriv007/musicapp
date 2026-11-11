package com.musicapp.browse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DecadesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @ParameterizedTest
    @CsvSource({"1950, 50s", "1990, 90s", "2000, 00s", "2010, 10s", "2020, 20s"})
    void labels(int decade, String label) {
        assertThat(Decades.label(decade)).isEqualTo(label);
    }

    @ParameterizedTest
    @CsvSource({"1999, 1990", "2000, 2000", "2026, 2020", "1955, 1950", "1931, 1950"})
    void decadeOfYear(int year, int decade) {
        assertThat(Decades.decadeOf(year)).isEqualTo(decade);
    }

    @Test
    void allDecadesNewestFirst() {
        assertThat(Decades.all(TODAY)).containsExactly(2020, 2010, 2000, 1990, 1980, 1970, 1960, 1950);
    }

    @Test
    void ranges() {
        assertThat(Decades.range("latest", TODAY)).isEqualTo(new Decades.DateRange(LocalDate.of(2025, 6, 15), null));
        assertThat(Decades.range("1990", TODAY))
                .isEqualTo(new Decades.DateRange(LocalDate.of(1990, 1, 1), LocalDate.of(1999, 12, 31)));
        assertThat(Decades.range("1950", TODAY)).isEqualTo(new Decades.DateRange(null, LocalDate.of(1959, 12, 31)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1995", "1940", "2030", "nineties"})
    void rejectsUnknownDecades(String key) {
        assertThatThrownBy(() -> Decades.range(key, TODAY)).isInstanceOf(IllegalArgumentException.class);
    }
}

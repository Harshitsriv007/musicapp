package com.musicapp.ingestion.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderSupportTest {

    @ParameterizedTest
    @ValueSource(strings = {"2024-05-01", "2024-05-01T10:00:00Z", "2024-05-01T10:00:00+05:30", "2024-05-01 10:00:00"})
    void parsesCommonDateFormats(String raw) {
        assertThat(ProviderSupport.parseDate(raw)).isEqualTo(LocalDate.of(2024, 5, 1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Thu Feb 25 2021", "garbage"})
    void returnsNullForUnparseableDates(String raw) {
        assertThat(ProviderSupport.parseDate(raw)).isNull();
    }

    @Test
    void truncatesLongValues() {
        assertThat(ProviderSupport.truncate("  abcdef ", 3)).isEqualTo("abc");
        assertThat(ProviderSupport.truncate(null, 3)).isNull();
    }
}

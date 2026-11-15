package com.codecool.flightapi.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ZoneIdConverterTest {

    private final ZoneIdConverter converter = new ZoneIdConverter();

    @Test
    void storesTheZoneIdAndReadsItBack() {
        assertThat(converter.convertToDatabaseColumn(ZoneId.of("Europe/London")))
                .isEqualTo("Europe/London");
        assertThat(converter.convertToEntityAttribute("Europe/London")).isEqualTo(ZoneId.of("Europe/London"));
    }

    @Test
    void keepsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

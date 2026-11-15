package com.codecool.flightapi.airport;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.city.CityMapperImpl;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AirportMapperTest {

    private final AirportMapper mapper = new AirportMapperImpl(new CityMapperImpl());

    @Test
    void mapsTimeZonesByIdAndKeepsNull() {
        assertThat(mapper.zoneId(ZoneId.of("Europe/Paris"))).isEqualTo("Europe/Paris");
        assertThat(mapper.zoneId("Europe/Paris")).isEqualTo(ZoneId.of("Europe/Paris"));
        assertThat(mapper.zoneId((ZoneId) null)).isNull();
        assertThat(mapper.zoneId((String) null)).isNull();
    }
}

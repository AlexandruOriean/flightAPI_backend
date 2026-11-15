package com.codecool.flightapi.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.ZoneId;

/** Stores a {@link ZoneId} as its IANA id, e.g. {@code Europe/Bucharest}. */
@Converter(autoApply = true)
public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId zone) {
        return zone == null ? null : zone.getId();
    }

    @Override
    public ZoneId convertToEntityAttribute(String id) {
        return id == null ? null : ZoneId.of(id);
    }
}

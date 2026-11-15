package com.codecool.flightapi.common.persistence;

import db.migration.LegacyDataCheck;
import org.flywaydb.core.api.callback.Callback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Flyway picks up {@link Callback} beans; this one checks old databases before migrating them. */
@Configuration(proxyBeanMethods = false)
class MigrationConfig {

    @Bean
    Callback legacyDataCheck() {
        return new LegacyDataCheck();
    }
}

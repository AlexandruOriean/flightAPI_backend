package com.codecool.flightapi.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The application's clock (replaceable in tests) and scheduled jobs, such as the refresh-token cleanup. */
@Configuration
@EnableScheduling
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

package com.codecool.flightapi.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.common.error.PreconditionFailedException;
import com.codecool.flightapi.common.error.PreconditionRequiredException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ETagsTest {

    @Test
    @DisplayName("an ETag is the version as a strong, quoted tag")
    void etagIsTheQuotedVersion() {
        assertThat(ETags.of(3)).isEqualTo("\"3\"");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("a missing or blank If-Match is 428")
    void missingIfMatchIsRequired(String header) {
        assertThatThrownBy(() -> ETags.ifMatch(header)).isInstanceOf(PreconditionRequiredException.class);
    }

    @Test
    @DisplayName("If-Match with one or more strong tags matches exactly those versions")
    void strongTagsMatchTheirVersions() {
        assertThat(ETags.ifMatch("\"3\"").versions()).containsExactly(3L);
        assertThat(ETags.ifMatch(" \"3\" , \"5\"").versions()).isEqualTo(Set.of(3L, 5L));

        ETags.IfMatch ifMatch = ETags.ifMatch("\"3\"");
        assertThatCode(() -> ifMatch.requireMatches(3)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ifMatch.requireMatches(4)).isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    @DisplayName("If-Match: * matches any version")
    void starMatchesAnything() {
        assertThat(ETags.ifMatch(" * ")).isEqualTo(ETags.IfMatch.ANY);
        assertThatCode(() -> ETags.IfMatch.ANY.requireMatches(42)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"W/\"3\"", "3", "\"abc\"", "\"3"})
    @DisplayName("weak, unquoted or foreign tags never match (strong comparison), so the update is 412")
    void otherTagsNeverMatch(String header) {
        assertThatThrownBy(() -> ETags.ifMatch(header).requireMatches(3))
                .isInstanceOf(PreconditionFailedException.class);
    }
}

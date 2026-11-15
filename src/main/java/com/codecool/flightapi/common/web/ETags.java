package com.codecool.flightapi.common.web;

import com.codecool.flightapi.common.error.PreconditionFailedException;
import com.codecool.flightapi.common.error.PreconditionRequiredException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity tags for single resources: the entity's optimistic-lock version as a strong ETag, e.g. {@code "3"}.
 * Clients send it back in {@code If-Match} when they update, so they never overwrite changes they have not seen.
 */
public final class ETags {

    private ETags() {}

    /** The ETag header value for a resource at this version. */
    public static String of(long version) {
        return "\"" + version + "\"";
    }

    /**
     * Parses an {@code If-Match} header: {@code *}, or one or more strong ETags separated by commas.
     *
     * @throws PreconditionRequiredException if the header is missing or blank
     */
    public static IfMatch ifMatch(String header) {
        if (header == null || header.isBlank()) {
            throw new PreconditionRequiredException();
        }
        if (header.strip().equals("*")) {
            return IfMatch.ANY;
        }
        // weak (W/"..") or foreign tags can never match a strong version tag, so they are simply dropped
        Set<Long> versions = Arrays.stream(header.split(","))
                .map(String::strip)
                .filter(tag -> tag.matches("\"\\d{1,18}\""))
                .map(tag -> Long.valueOf(tag.substring(1, tag.length() - 1)))
                .collect(Collectors.toUnmodifiableSet());
        return new IfMatch(versions, false);
    }

    /**
     * A parsed {@code If-Match} header.
     *
     * @param versions the versions the client last saw
     * @param any {@code *}: any current version is fine
     */
    public record IfMatch(Set<Long> versions, boolean any) {

        /** {@code If-Match: *}. */
        public static final IfMatch ANY = new IfMatch(Set.of(), true);

        /**
         * Checks the resource's current version against the client's.
         *
         * @throws PreconditionFailedException if the resource changed since the client read it
         */
        public void requireMatches(long currentVersion) {
            if (!any && !versions.contains(currentVersion)) {
                throw new PreconditionFailedException();
            }
        }
    }
}

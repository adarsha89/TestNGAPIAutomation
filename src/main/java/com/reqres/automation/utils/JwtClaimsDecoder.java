package com.reqres.automation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the {@code exp} claim (epoch seconds) out of a JWT's compact
 * serialization for cache-expiry bookkeeping - base64url-decodes the
 * payload segment (segment 2 of the dot-separated token) and parses it as
 * JSON via the already-present {@code jackson-databind}.
 * <p>
 * <b>Deliberate limitation:</b> this does not verify the JWT's signature.
 * It is used only to read a claim for cache bookkeeping, not to assert
 * token authenticity - out of scope per the requirements doc.
 */
public final class JwtClaimsDecoder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtClaimsDecoder() {
    }

    /** Returns the decoded {@code exp} claim as an {@link Instant}, or empty if the token is
     * malformed, isn't a 3-segment compact JWT, or has no numeric {@code exp} claim. */
    public static Optional<Instant> decodeExpiry(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return Optional.empty();
        }
        String[] segments = jwt.split("\\.");
        if (segments.length != 3) {
            return Optional.empty();
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(segments[1]);
            Map<String, Object> claims = OBJECT_MAPPER.readValue(payloadBytes, Map.class);
            Object exp = claims.get("exp");
            if (!(exp instanceof Number)) {
                return Optional.empty();
            }
            return Optional.of(Instant.ofEpochSecond(((Number) exp).longValue()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

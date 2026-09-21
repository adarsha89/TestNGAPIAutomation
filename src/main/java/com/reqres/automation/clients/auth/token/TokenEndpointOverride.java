package com.reqres.automation.clients.auth.token;

/**
 * Test-support-only, {@code ThreadLocal}-backed override letting a test
 * point a token-issuing strategy at its local WireMock stub's dynamic port
 * for the current thread, rather than the configured
 * {@code <endpointKey>.auth.token.endpoint} property - mirrors
 * {@code FallbackHelper}/{@code LogMasker}'s existing test-support statics.
 * When unset, callers resolve the real, statically-configured issuer URL.
 */
public final class TokenEndpointOverride {

    private static final ThreadLocal<String> OVERRIDE_URL = new ThreadLocal<>();

    private TokenEndpointOverride() {
    }

    public static void set(String tokenEndpointUrl) {
        OVERRIDE_URL.set(tokenEndpointUrl);
    }

    public static void clear() {
        OVERRIDE_URL.remove();
    }

    // package-private: only the auth strategy layer resolves this, never test code directly
    static String get() {
        return OVERRIDE_URL.get();
    }
}

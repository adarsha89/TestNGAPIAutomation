package com.reqres.automation.testdata;

import java.util.UUID;

/**
 * Test-only builder for unique, synthetic provided-IDs per call so tests never depend on a
 * hardcoded ID or leftover state from a previous run against the CSV-backed
 * {@code ProvidedIdStore} (NFR-1/A-3). Mirrors {@link UserDataBuilder#uniqueUser()}.
 */
public final class ProvidedIdDataBuilder {

    private ProvidedIdDataBuilder() {
    }

    public static String uniqueId() {
        return "provided-id-" + UUID.randomUUID();
    }
}

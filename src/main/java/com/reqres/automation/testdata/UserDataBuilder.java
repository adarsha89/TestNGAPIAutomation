package com.reqres.automation.testdata;

import com.reqres.automation.models.rest.UserRequest;

import java.util.UUID;

/**
 * Builds unique/synthetic user payloads per run so tests never depend on
 * hardcoded, production-shaped data or leftover state from a previous run.
 */
public final class UserDataBuilder {

    private UserDataBuilder() {
    }

    public static UserRequest uniqueUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new UserRequest("qa-user-" + suffix, "automation-" + suffix);
    }
}

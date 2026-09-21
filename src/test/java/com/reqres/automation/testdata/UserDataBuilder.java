package com.reqres.automation.testdata;

import com.reqres.automation.models.rest.User;

import java.util.UUID;

/**
 * Test-only builder for unique/synthetic user payloads per run so tests never depend on
 * hardcoded, production-shaped data or leftover state from a previous run.
 */
public final class UserDataBuilder {

    private UserDataBuilder() {
    }

    public static User uniqueUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.builder()
                .name("qa-user-" + suffix)
                .job("automation-" + suffix)
                .build();
    }
}

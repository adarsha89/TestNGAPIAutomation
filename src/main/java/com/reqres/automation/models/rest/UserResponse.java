package com.reqres.automation.models.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for {@code GET /api/users/{id}}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    private User data;
    private Support support;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Support {
        private String url;
        private String text;
    }
}

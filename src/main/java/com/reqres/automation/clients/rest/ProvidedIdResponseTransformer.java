package com.reqres.automation.clients.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

/**
 * WireMock {@link ResponseDefinitionTransformer} that gives
 * {@link ProvidedIdMockServer} real per-request logic instead of a fixed canned response: it
 * parses {@code {"id": "..."}} from the request body, delegates the duplicate check to a
 * {@link ProvidedIdStore}, and returns 201 {@code {"id":...,"status":"accepted"}} the first time
 * an ID is seen, or 400 {@code {"id":...,"status":"duplicate"}} on any resubmission.
 *
 * <p>A missing/blank {@code id} is handled defensively - 400 {@code {"error":"id is required"}}
 * without touching the store - so the mock never crashes or corrupts its state on a malformed
 * request, though this path isn't covered by a test in this plan (see the plan's open
 * questions).</p>
 */
public class ProvidedIdResponseTransformer extends ResponseDefinitionTransformer {

    public static final String NAME = "provided-id-transformer";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProvidedIdStore store;

    public ProvidedIdResponseTransformer(ProvidedIdStore store) {
        this.store = store;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files,
            Parameters parameters) {
        String id = extractId(request.getBodyAsString());
        if (id == null || id.isBlank()) {
            return jsonResponse(400, "{\"error\":\"id is required\"}");
        }

        boolean recordedNow = store.recordIfAbsent(id);
        if (recordedNow) {
            return jsonResponse(201, "{\"id\":\"" + id + "\",\"status\":\"accepted\"}");
        }
        return jsonResponse(400, "{\"id\":\"" + id + "\",\"status\":\"duplicate\"}");
    }

    private static String extractId(String requestBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);
            JsonNode idNode = root.get("id");
            return (idNode == null || idNode.isNull()) ? null : idNode.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private static ResponseDefinition jsonResponse(int status, String jsonBody) {
        return WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)
                .build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}

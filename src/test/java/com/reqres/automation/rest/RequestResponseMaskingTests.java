package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.LogMasker;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Verifies the request/response logging masking behavior required by
 * {@code docs/requirements/request-response-logging-masking-requirements.md}:
 * sensitive headers/query params are masked in what actually reaches the
 * Allure report, non-sensitive ones are left unmasked, and the real
 * (unmasked) values still reach the API.
 */
@Story("Request/response logging masking")
public class RequestResponseMaskingTests implements BaseRestInterface {

    private static final String MASKED_VALUE = "***MASKED***";
    private static final String SENSITIVE_HEADER_NAME = "Authorization";
    private static final String SENSITIVE_HEADER_VALUE = "Bearer super-secret-token-value";
    private static final String NON_SENSITIVE_HEADER_NAME = "X-Correlation-Id";
    private static final String NON_SENSITIVE_HEADER_VALUE = "correlation-12345";
    private static final String SENSITIVE_QUERY_PARAM_NAME = "token";
    private static final String SENSITIVE_QUERY_PARAM_VALUE = "super-secret-query-token";
    private static final String NON_SENSITIVE_QUERY_PARAM_NAME = "page";
    private static final String NON_SENSITIVE_QUERY_PARAM_VALUE = "1";

    @Test(groups = {"rest", "regression"})
    @Description("A sensitive header/query param is masked and a non-sensitive header/query param is left "
            + "unmasked in the Allure-attached request/response record, while the real values still reach the API")
    public void shouldMaskSensitiveDataInAllureAttachmentsWithoutAffectingTheRealCall() throws IOException {
        Map<String, String> extraHeaders = new LinkedHashMap<>();
        extraHeaders.put(SENSITIVE_HEADER_NAME, SENSITIVE_HEADER_VALUE);
        extraHeaders.put(NON_SENSITIVE_HEADER_NAME, NON_SENSITIVE_HEADER_VALUE);

        Map<String, String> extraQueryParams = new LinkedHashMap<>();
        extraQueryParams.put(SENSITIVE_QUERY_PARAM_NAME, SENSITIVE_QUERY_PARAM_VALUE);
        extraQueryParams.put(NON_SENSITIVE_QUERY_PARAM_NAME, NON_SENSITIVE_QUERY_PARAM_VALUE);

        restService().getUserByIdWithExtraParamsAndVerify(2, extraHeaders, extraQueryParams,
                ResponseExpectation.status(200).andBodyValueEquals("data.id", 2));

        String correlationId = LogMasker.MaskingLoggingFilter.lastCorrelationId();
        Assert.assertNotNull(correlationId,
                "Expected the masking filter to have recorded a correlation id for this call");

        List<String> newAttachmentContents = readAllureAttachmentsForCorrelationId(correlationId);
        Assert.assertEquals(newAttachmentContents.size(), 2,
                "Expected exactly one request and one response attachment for correlation id "
                        + correlationId + ", found: " + newAttachmentContents.size());

        String combined = String.join("\n---\n", newAttachmentContents);

        Assert.assertTrue(combined.contains(SENSITIVE_HEADER_NAME + "=" + MASKED_VALUE),
                "Expected sensitive header to be masked in the attachment, got:\n" + combined);
        Assert.assertFalse(combined.contains(SENSITIVE_HEADER_VALUE),
                "Real sensitive header value must not appear in the attachment, got:\n" + combined);

        Assert.assertTrue(combined.contains(SENSITIVE_QUERY_PARAM_NAME + "=" + MASKED_VALUE),
                "Expected sensitive query param to be masked in the attachment, got:\n" + combined);
        Assert.assertFalse(combined.contains(SENSITIVE_QUERY_PARAM_VALUE),
                "Real sensitive query param value must not appear in the attachment, got:\n" + combined);

        Assert.assertTrue(combined.contains(NON_SENSITIVE_HEADER_NAME + "=" + NON_SENSITIVE_HEADER_VALUE),
                "Expected non-sensitive header to remain unmasked in the attachment, got:\n" + combined);

        Assert.assertTrue(combined.contains(NON_SENSITIVE_QUERY_PARAM_NAME + "=" + NON_SENSITIVE_QUERY_PARAM_VALUE),
                "Expected non-sensitive query param to remain unmasked in the attachment, got:\n" + combined);
    }

    @Test(groups = {"rest", "regression"})
    @Description("LogMasker.isSensitive/maskValue correctly identify and mask sensitive names, and pass through "
            + "non-sensitive names unmasked - the same method used for both request and response header masking")
    public void shouldIdentifyAndMaskSensitiveNamesConsistently() {
        Assert.assertTrue(LogMasker.isSensitive("authorization"));
        Assert.assertTrue(LogMasker.isSensitive("Authorization"));
        Assert.assertEquals(LogMasker.maskValue("authorization", "secret-value"), MASKED_VALUE);

        Assert.assertFalse(LogMasker.isSensitive(NON_SENSITIVE_HEADER_NAME));
        Assert.assertEquals(LogMasker.maskValue(NON_SENSITIVE_HEADER_NAME, NON_SENSITIVE_HEADER_VALUE),
                NON_SENSITIVE_HEADER_VALUE);

        for (String sensitiveName : Constants.SENSITIVE_DATA) {
            Assert.assertTrue(LogMasker.isSensitive(sensitiveName),
                    "Expected '" + sensitiveName + "' from Constants.SENSITIVE_DATA to be treated as sensitive");
        }
    }

    private List<String> readAllureAttachmentsForCorrelationId(String correlationId) throws IOException {
        Path resultsDir = Path.of("target", "allure-results");
        if (!Files.isDirectory(resultsDir)) {
            return List.of();
        }
        String marker = "Correlation-Id: " + correlationId;
        try (Stream<Path> files = Files.list(resultsDir)) {
            return files
                    .filter(path -> path.getFileName().toString().contains("-attachment"))
                    .map(this::readFileQuietly)
                    .filter(content -> content.contains(marker))
                    .toList();
        }
    }

    private String readFileQuietly(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }
}

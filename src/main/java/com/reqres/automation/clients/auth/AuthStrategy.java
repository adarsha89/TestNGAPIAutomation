package com.reqres.automation.clients.auth;

import com.reqres.automation.config.EnvConfig;
import io.restassured.builder.RequestSpecBuilder;

/**
 * A per-endpoint-key credential-application mechanism. Resolved by
 * {@code AuthStrategyFactory} from the {@code <endpointKey>.auth.strategy}
 * config property and applied to a not-yet-built
 * {@link RequestSpecBuilder} before it is layered with protocol-specific
 * customization.
 */
public interface AuthStrategy {

    void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey);
}

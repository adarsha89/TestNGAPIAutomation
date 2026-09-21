package com.reqres.automation.clients;

import com.reqres.automation.config.EnvConfig;

/**
 * Common lifecycle contract implemented by every protocol client
 * (rest/webhook). Protocol-specific request methods
 * live on each concrete implementation - this interface only standardizes
 * how a client is initialized/torn down so {@link ClientFactory} can build
 * any of them uniformly.
 */
public interface ApiClient {

    /**
     * Initializes the client from the resolved environment configuration
     * (base URL, auth, timeouts, etc).
     */
    void init(EnvConfig config);

    /**
     * Releases any resources held by the client (connections, servers).
     */
    void close();
}

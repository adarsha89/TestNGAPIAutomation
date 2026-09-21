package com.reqres.automation.config;

import com.reqres.automation.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Resolves the active environment (-Denv=qa|staging|prod, default qa) and
 * builds its configuration in layers, each taking priority over the last:
 * <ol>
 *     <li>{@code src/test/resources/config/common.properties} - defaults
 *     that hold for every environment (timeouts, ports, etc.) and have no
 *     reason to vary by env;</li>
 *     <li>{@code src/test/resources/config/<env>.properties} - values that
 *     are genuinely environment-specific (base URLs), overriding the common
 *     defaults where the same key appears in both;</li>
 *     <li>an optional, gitignored {@code <env>.local.properties} on the
 *     classpath, for local-only values (e.g. a real API key) that must
 *     never be committed;</li>
 *     <li>an identically-named (by convention) environment variable for
 *     any property - e.g. {@code rest.base.url} is overridden by
 *     {@code REST_BASE_URL}, and {@code api.key} by {@code API_KEY}. This
 *     is how the key that used to live in the untracked {@code APIKey.txt}
 *     at the repo root should be supplied at runtime.</li>
 * </ol>
 */
public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static EnvConfig load() {
        String env = System.getProperty(Constants.ENV_SYSTEM_PROPERTY, Constants.DEFAULT_ENV);

        Properties properties = new Properties();
        loadResourceInto(properties, Constants.CONFIG_DIR + Constants.COMMON_CONFIG_FILE, true);
        loadResourceInto(properties, Constants.CONFIG_DIR + env + ".properties", true);
        loadResourceInto(properties, Constants.CONFIG_DIR + env + ".local.properties", false);

        applyEnvironmentOverrides(properties);

        return new EnvConfig(env, properties);
    }

    private static void loadResourceInto(Properties properties, String resource, boolean required) {
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Missing required config file on classpath: " + resource);
                }
                return;
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config file: " + resource, e);
        }
    }

    private static void applyEnvironmentOverrides(Properties properties) {
        // Override any already-known property if an env var of the
        // conventional name is set.
        for (String key : properties.stringPropertyNames()) {
            applyOverrideIfPresent(properties, key);
        }
        // The API key may be supplied purely via env var with no
        // placeholder present at all in the properties file.
        applyOverrideIfPresent(properties, Constants.API_KEY_PROPERTY);
    }

    private static void applyOverrideIfPresent(Properties properties, String propertyKey) {
        String envVarName = toEnvVarName(propertyKey);
        String override = System.getenv(envVarName);
        if (override != null && !override.isBlank()) {
            properties.setProperty(propertyKey, override);
        }
    }

    private static String toEnvVarName(String propertyKey) {
        return propertyKey.toUpperCase(Locale.ROOT).replace('.', '_');
    }
}

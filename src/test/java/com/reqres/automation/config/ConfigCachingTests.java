package com.reqres.automation.config;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link ConfigLoader#load()} caches: a second call for the same
 * {@code -Denv} value within this run doesn't perform a second classpath
 * read. {@code ConfigLoader.getClasspathReadCount()} is test-support
 * instrumentation with no other consumer.
 */
@Story("ConfigLoader.load() caches by resolved environment")
public class ConfigCachingTests {

    @Test(groups = {"config", "cache", "regression"})
    @Description("A repeated ConfigLoader.load() call for the same env within a run does not increase the "
            + "classpath read count, and returns a value-equal EnvConfig both times")
    public void shouldNotReReadClasspathOnRepeatedLoadForSameEnv() {
        EnvConfig first = ConfigLoader.load();
        int readCountAfterFirst = ConfigLoader.getClasspathReadCount();

        EnvConfig second = ConfigLoader.load();
        int readCountAfterSecond = ConfigLoader.getClasspathReadCount();

        Assert.assertEquals(readCountAfterSecond, readCountAfterFirst,
                "Expected no additional classpath read on the second ConfigLoader.load() call for the same env");
        Assert.assertEquals(second.getEnvironment(), first.getEnvironment());
        Assert.assertEquals(second.getRestBaseUrl(), first.getRestBaseUrl());
    }
}

package com.reqres.automation.config;

import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.LogMasker;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;
import java.util.Set;

// FR10: sensitive-name list comes from config (sensitive.data.names), falling back to
// Constants.SENSITIVE_DATA. Builds EnvConfig directly via its package-private constructor
// with hand-built Properties, so no classpath resource file is needed here.
@Story("Sensitive data names sourced from config")
public class EnvConfigSensitiveDataTests {

    @Test(groups = {"config", "regression"})
    @Description("EnvConfig.getSensitiveDataNames() resolves the exact set parsed from a "
            + "sensitive.data.names property (comma-split, trimmed, lower-cased)")
    public void shouldResolveSensitiveDataNamesFromConfiguredProperty() {
        Properties properties = new Properties();
        properties.setProperty(Constants.SENSITIVE_DATA_PROPERTY, "custom-secret, Another-One");

        EnvConfig config = new EnvConfig("qa", properties);

        Assert.assertEquals(config.getSensitiveDataNames(), Set.of("custom-secret", "another-one"));
    }

    @Test(groups = {"config", "regression"})
    @Description("EnvConfig.getSensitiveDataNames() falls back to the compiled-in Constants.SENSITIVE_DATA "
            + "default when the sensitive.data.names property is absent")
    public void shouldFallBackToCompiledDefaultWhenPropertyAbsent() {
        Properties properties = new Properties();

        EnvConfig config = new EnvConfig("qa", properties);

        Assert.assertEquals(config.getSensitiveDataNames(), Constants.SENSITIVE_DATA);
    }

    @Test(groups = {"config", "regression"})
    @Description("EnvConfig.getSensitiveDataNames() falls back to the compiled-in Constants.SENSITIVE_DATA "
            + "default when the sensitive.data.names property is present but blank")
    public void shouldFallBackToCompiledDefaultWhenPropertyBlank() {
        Properties properties = new Properties();
        properties.setProperty(Constants.SENSITIVE_DATA_PROPERTY, "");

        EnvConfig config = new EnvConfig("qa", properties);

        Assert.assertEquals(config.getSensitiveDataNames(), Constants.SENSITIVE_DATA);
    }

    @Test(groups = {"config", "regression"})
    @Description("A name added only via config (not present in the compiled default) is masked once resolved, "
            + "proving FR10's 'add a new sensitive name via config, no code change' acceptance criterion")
    public void shouldMaskOnlyNamesPresentInTheResolvedSet() {
        Properties properties = new Properties();
        properties.setProperty(Constants.SENSITIVE_DATA_PROPERTY, "custom-secret");

        EnvConfig config = new EnvConfig("qa", properties);
        Set<String> resolvedSet = config.getSensitiveDataNames();

        Assert.assertTrue(LogMasker.isSensitive("custom-secret", resolvedSet),
                "Expected 'custom-secret' to be sensitive against the config-resolved set");
        Assert.assertFalse(LogMasker.isSensitive("custom-secret", Constants.SENSITIVE_DATA),
                "Expected 'custom-secret' to not be sensitive against the compiled default alone");
    }
}

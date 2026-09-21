package com.reqres.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.fixtures.CountingFixture;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Coverage for {@link NestedRunSandbox}'s classpath-construction mechanism
 * (Functional Requirements 16-18 of the iteration-2 plan): proves the
 * resolved classpath is sourced entirely from the build-tool-resolved
 * {@code target/test-classpath.txt} file, never from the parent JVM's own
 * {@code java.class.path} runtime property, and that the resulting
 * {@code @argfile}-based subprocess launch remains correct under a
 * representative long/expanded classpath well past common OS command-line
 * length ceilings.
 */
@Story("Subprocess classpath construction sourced from the build-tool-resolved classpath file")
public class NestedRunSandboxClasspathTests {

    private static final Path CLASSPATH_FILE =
            Path.of(System.getProperty("user.dir"), "target/test-classpath.txt");

    @Test(groups = {"utils", "regression"})
    @Description("resolveEffectiveClasspath()'s output is unaffected by mutating java.class.path, and does change "
            + "when target/test-classpath.txt's own contents change - proving the mechanism structurally cannot "
            + "regress to reading the parent JVM's own runtime classpath property")
    public void shouldResolveClasspathIndependentlyOfJavaClassPath() throws IOException {
        String originalJavaClassPath = System.getProperty("java.class.path");
        Path tempDir = Files.createTempDirectory("nested-run-sandbox-classpath-independence");
        Path tempCopy = tempDir.resolve("test-classpath.txt");
        Files.copy(CLASSPATH_FILE, tempCopy);
        try {
            String baseline = NestedRunSandbox.resolveEffectiveClasspath(tempCopy);

            System.setProperty("java.class.path", "/nonexistent/bogus/path/that/should/be/ignored.jar");
            String afterMutatingJavaClassPath = NestedRunSandbox.resolveEffectiveClasspath(tempCopy);
            Assert.assertEquals(afterMutatingJavaClassPath, baseline,
                    "resolveEffectiveClasspath() must be unaffected by java.class.path");

            String originalFileContent = Files.readString(tempCopy, StandardCharsets.UTF_8);
            String mutatedFileContent = originalFileContent + File.pathSeparator + "/synthetic/marker/entry.jar";
            Files.writeString(tempCopy, mutatedFileContent, StandardCharsets.UTF_8);
            String afterMutatingFile = NestedRunSandbox.resolveEffectiveClasspath(tempCopy);
            Assert.assertNotEquals(afterMutatingFile, baseline,
                    "resolveEffectiveClasspath() must change when the consulted file's contents change");
            Assert.assertTrue(afterMutatingFile.contains("/synthetic/marker/entry.jar"));
        } finally {
            System.setProperty("java.class.path", originalJavaClassPath);
        }
    }

    @Test(groups = {"utils", "regression"})
    @Description("Appending several hundred synthetic classpath entries so the resulting -cp value comfortably "
            + "exceeds 32,000 characters, the @argfile-based subprocess launch still completes correctly and the "
            + "written summary reports CountingFixture's actual outcome (2 PASS / 1 FAIL)")
    public void shouldRunCorrectlyUnderARepresentativeLongClasspath() throws IOException {
        List<String> syntheticEntries = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            syntheticEntries.add("/synthetic/nonexistent/classpath/entry/number-" + i + "-padding-to-inflate-length");
        }

        String baseClasspath = NestedRunSandbox.resolveEffectiveClasspath();
        int approximateAdditionalLength = 0;
        for (String entry : syntheticEntries) {
            approximateAdditionalLength += entry.length() + File.pathSeparator.length();
        }
        Assert.assertTrue(baseClasspath.length() + approximateAdditionalLength > 32_000,
                "Test setup should produce a resulting -cp value over 32,000 characters, base classpath was "
                        + baseClasspath.length() + " chars, synthetic addition was " + approximateAdditionalLength
                        + " chars");

        Path workingDir = NestedRunSandbox.runFixtureInSubprocess(CountingFixture.class, syntheticEntries);

        File summaryFile = workingDir.resolve("target/test-result-summary.json").toFile();
        Assert.assertTrue(summaryFile.isFile(),
                "Expected CustomTestNGListener#onFinish to have written " + summaryFile);

        JsonNode counts = new ObjectMapper().readTree(summaryFile).get("counts");
        Assert.assertEquals(counts.get("PASS").asLong(), 2L);
        Assert.assertEquals(counts.get("FAIL").asLong(), 1L);
        Assert.assertEquals(counts.get("SKIP").asLong(), 0L);
    }

    @Test(groups = {"utils", "regression"})
    @Description("When target/test-classpath.txt is absent (e.g. the build phase that generates it was skipped), "
            + "resolveEffectiveClasspath() fails fast with a clear IllegalStateException naming the missing file "
            + "and the remediation, rather than silently falling back to java.class.path")
    public void shouldFailFastWhenClasspathFileIsMissing() throws IOException {
        Path tempDir = Files.createTempDirectory("nested-run-sandbox-classpath-missing");
        Path neverCreated = tempDir.resolve("test-classpath.txt");

        IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
                () -> NestedRunSandbox.resolveEffectiveClasspath(neverCreated));
        Assert.assertTrue(exception.getMessage().contains("test-classpath.txt"),
                "Expected the exception message to name the missing file, was: " + exception.getMessage());
        Assert.assertTrue(exception.getMessage().contains("mvn generate-test-resources"),
                "Expected the exception message to name the remediation, was: " + exception.getMessage());
    }
}

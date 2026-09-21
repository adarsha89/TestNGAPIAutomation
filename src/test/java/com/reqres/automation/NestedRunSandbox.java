package com.reqres.automation;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test-support only. Drives a standalone TestNG run of a fixture class in a
 * separate JVM process, used by {@code TestResultAggregatorIntegrationTests}
 * / {@code FlakyTestDetectorTests} to observe a real
 * {@link CustomTestNGListener} run. The child process writes
 * {@code target/test-result-summary.json} (relative to its own working
 * directory, an isolated temp dir) via the real {@link CustomTestNGListener}
 * wiring, which the caller then reads back.
 * <p>
 * Fixture classes driven this way must live in their own top-level file
 * outside any {@code *Tests.java}-pattern file (see
 * {@code com.reqres.automation.fixtures}): Surefire's default discovery also
 * picks up a fixture nested inside a {@code *Tests.java} file and runs it as
 * part of the real suite, which would fold its deliberate failure into the
 * build.
 * <p>
 * The child process's classpath is sourced from the build-tool-resolved
 * {@code target/test-classpath.txt} file (written by {@code maven-dependency-
 * plugin}'s {@code build-classpath} goal during the {@code generate-test-
 * resources} phase), combined with this module's own fixed output
 * directories - never from the parent JVM's own {@code java.class.path}
 * runtime property, which only reflects however the parent JVM itself
 * happened to be launched and can silently collapse to a single manifest-only
 * jar path once the real classpath exceeds OS command-line length limits.
 */
final class NestedRunSandbox {

    private static final String CLASSPATH_FILE_RELATIVE_PATH = "target/test-classpath.txt";

    private NestedRunSandbox() {
    }

    /** Runs {@code fixtureClass} to completion via {@code org.testng.TestNG}'s CLI entry point in
     * a fresh JVM whose working directory is a new isolated temp dir, with {@link CustomTestNGListener}
     * registered. Returns the temp dir the child wrote {@code target/test-result-summary.json} into. */
    static Path runFixtureInSubprocess(Class<?> fixtureClass) {
        return runFixtureInSubprocess(fixtureClass, Collections.emptyList());
    }

    /** Same as {@link #runFixtureInSubprocess(Class)}, but appends {@code syntheticExtraClasspathEntries}
     * to the resolved classpath before launching the child JVM - used to exercise the {@code @argfile}-based
     * launch under a representative long/expanded classpath without depending on the real dependency set
     * happening to be long enough locally. */
    static Path runFixtureInSubprocess(Class<?> fixtureClass, List<String> syntheticExtraClasspathEntries) {
        return runFixtureInSubprocess(fixtureClass, syntheticExtraClasspathEntries, Map.of());
    }

    /** Same as {@link #runFixtureInSubprocess(Class)}, but also passes {@code extraSystemProperties} as
     * {@code -D} flags to the child JVM - used to steer a fixture's behavior (e.g. {@code FlappyFixture}'s
     * pass/fail outcome) or override a component's defaults (e.g. {@code RunHistoryStore}'s directory) for a
     * given subprocess run. */
    static Path runFixtureInSubprocess(Class<?> fixtureClass, Map<String, String> extraSystemProperties) {
        return runFixtureInSubprocess(fixtureClass, Collections.emptyList(), extraSystemProperties);
    }

    private static Path runFixtureInSubprocess(Class<?> fixtureClass, List<String> syntheticExtraClasspathEntries,
            Map<String, String> extraSystemProperties) {
        try {
            Path workingDir = Files.createTempDirectory("nested-testng-run");

            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = buildLaunchClasspath(syntheticExtraClasspathEntries);

            Path argfile = writeArgfile(workingDir, classpath, fixtureClass, extraSystemProperties);

            ProcessBuilder processBuilder = new ProcessBuilder(javaBin, "@" + argfile);
            processBuilder.directory(workingDir.toFile());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(workingDir.resolve("subprocess-output.log").toFile());

            Process process = processBuilder.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Nested TestNG subprocess for " + fixtureClass.getName()
                        + " did not finish within the timeout");
            }

            return workingDir;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to run a nested TestNG subprocess for "
                    + fixtureClass.getName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a nested TestNG subprocess for "
                    + fixtureClass.getName(), e);
        }
    }

    private static String buildLaunchClasspath(List<String> syntheticExtraClasspathEntries) {
        String classpath = resolveEffectiveClasspath();
        if (syntheticExtraClasspathEntries.isEmpty()) {
            return classpath;
        }
        StringBuilder builder = new StringBuilder(classpath);
        for (String entry : syntheticExtraClasspathEntries) {
            builder.append(File.pathSeparatorChar).append(entry);
        }
        return builder.toString();
    }

    private static Path writeArgfile(Path workingDir, String classpath, Class<?> fixtureClass,
            Map<String, String> extraSystemProperties) throws IOException {
        Path argfile = workingDir.resolve("nested-run.argfile");
        List<String> tokens = new ArrayList<>();
        tokens.add("-cp");
        tokens.add(classpath);
        for (Map.Entry<String, String> property : extraSystemProperties.entrySet()) {
            tokens.add("-D" + property.getKey() + "=" + property.getValue());
        }
        tokens.add("org.testng.TestNG");
        tokens.add("-testclass");
        tokens.add(fixtureClass.getName());
        tokens.add("-listener");
        tokens.add(CustomTestNGListener.class.getName());
        tokens.add("-d");
        tokens.add(workingDir.resolve("test-output").toString());
        StringBuilder content = new StringBuilder();
        for (String token : tokens) {
            content.append('"').append(token.replace("\\", "\\\\").replace("\"", "\\\"")).append('"').append('\n');
        }
        Files.writeString(argfile, content.toString(), StandardCharsets.UTF_8);
        return argfile;
    }

    /** Resolves the classpath the child JVM should be launched with: the build-tool-resolved dependency
     * list written by {@code maven-dependency-plugin} to {@value #CLASSPATH_FILE_RELATIVE_PATH}, plus this
     * module's own fixed compiled-output directories. Never reads the parent JVM's {@code java.class.path}
     * runtime property - structurally cannot regress to that behavior. */
    static String resolveEffectiveClasspath() {
        return resolveEffectiveClasspath(Path.of(System.getProperty("user.dir"), CLASSPATH_FILE_RELATIVE_PATH));
    }

    /** Same resolution logic as {@link #resolveEffectiveClasspath()}, but reads from an explicit
     * {@code classpathFile} instead of the production default - exists so classpath-mutation/deletion
     * test cases can point this at an isolated, test-local file rather than the shared production file
     * other concurrently-scheduled test classes also depend on (see NestedRunSandboxClasspathTests). */
    static String resolveEffectiveClasspath(Path classpathFile) {
        if (!Files.isRegularFile(classpathFile)) {
            throw new IllegalStateException("Missing build-tool-resolved classpath file at " + classpathFile
                    + " - run `mvn generate-test-resources` (or a full build) first");
        }

        String dependencyClasspath;
        try {
            dependencyClasspath = Files.readString(classpathFile, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the build-tool-resolved classpath file at "
                    + classpathFile, e);
        }

        List<String> entries = new ArrayList<>();
        entries.add(Path.of(System.getProperty("user.dir"), "target/classes").toString());
        entries.add(Path.of(System.getProperty("user.dir"), "target/test-classes").toString());
        if (!dependencyClasspath.isEmpty()) {
            entries.add(dependencyClasspath);
        }

        return String.join(File.pathSeparator, entries);
    }
}

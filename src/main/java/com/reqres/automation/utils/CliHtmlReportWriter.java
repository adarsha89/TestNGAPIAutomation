package com.reqres.automation.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;

/** Writes a minimal, self-contained HTML report for a CLI command's output. */
public final class CliHtmlReportWriter {

    private CliHtmlReportWriter() {
    }

    /** Writes {@code items} as an HTML list under {@code title} to {@code outputFile}, creating
     * parent directories as needed, and returns {@code outputFile} for convenience. */
    public static Path write(Path outputFile, String title, Collection<String> items) {
        StringBuilder rows = new StringBuilder();
        if (items.isEmpty()) {
            rows.append("<li class=\"empty\">None</li>");
        } else {
            items.forEach(item -> rows.append("<li>").append(escape(item)).append("</li>"));
        }

        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escape(title) + "</title>"
                + "<style>"
                + "body{font-family:-apple-system,Segoe UI,sans-serif;margin:2rem;color:#1a1a1a}"
                + "h1{font-size:1.4rem;margin-bottom:0.25rem}"
                + ".meta{color:#666;margin-bottom:1rem;font-size:0.9rem}"
                + "ul{padding-left:1.2rem}"
                + "li{margin:0.25rem 0;font-family:monospace}"
                + "li.empty{color:#666;list-style:none;padding-left:0;font-family:inherit}"
                + "</style></head><body>"
                + "<h1>" + escape(title) + "</h1>"
                + "<div class=\"meta\">Generated " + Instant.now() + "</div>"
                + "<ul>" + rows + "</ul>"
                + "</body></html>";

        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write HTML report to " + outputFile, e);
        }
        return outputFile;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

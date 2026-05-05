package io.aether.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Simple template engine that replaces {{key}} placeholders in text resource files.
 */
public class TemplateEngine {

    /**
     * Loads a template from classpath resources and replaces placeholders.
     * @param path resource path relative to /templates/ (without leading slash)
     * @param variables map of key-value pairs to substitute
     * @return processed string
     * @throws IOException if resource not found
     */
    public static String render(String path, Map<String, String> variables) throws IOException {
        String template = loadResource("/templates/" + path);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream in = TemplateEngine.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Template not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

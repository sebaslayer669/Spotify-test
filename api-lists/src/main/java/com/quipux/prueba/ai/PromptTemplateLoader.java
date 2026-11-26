package com.quipux.prueba.ai;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt versionado en classpath (no improvisado en runtime).
 */
@Component
public class PromptTemplateLoader {

    private final ResourceLoader resourceLoader;
    private final AiProperties properties;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public PromptTemplateLoader(ResourceLoader resourceLoader, AiProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public String render(Map<String, String> variables) {
        String template = load(properties.getPromptResource());
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private String load(String location) {
        return cache.computeIfAbsent(location, loc -> {
            try {
                Resource resource = resourceLoader.getResource(loc);
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo cargar el prompt versionado: " + loc, e);
            }
        });
    }
}

package com.quipux.prueba.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parseo defensivo de la respuesta del LLM (markdown, vacío, JSON inválido).
 */
@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RecommendationItem> parseChatCompletionsBody(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new AiProviderException("Respuesta de IA vacía o inválida");
            }
            return parseRecommendationsJson(stripMarkdownFence(content.trim()));
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("No se pudo parsear la respuesta de IA", e);
        }
    }

    public List<RecommendationItem> parseRecommendationsJson(String json) {
        try {
            JsonNode parsed = objectMapper.readTree(json);
            JsonNode items = parsed.path("recommendations");
            if (!items.isArray()) {
                throw new AiProviderException("JSON de IA sin array recommendations");
            }
            if (items.isEmpty()) {
                throw new AiProviderException("JSON de IA con recommendations vacío");
            }
            List<RecommendationItem> result = new ArrayList<>();
            for (JsonNode node : items) {
                result.add(new RecommendationItem(
                        node.path("titulo").asText(""),
                        node.path("artista").asText(""),
                        node.path("genero").asText(""),
                        node.path("motivo").asText("")
                ));
            }
            return result;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("JSON de recomendaciones inválido", e);
        }
    }

    static String stripMarkdownFence(String content) {
        if (content.startsWith("```")) {
            int firstNl = content.indexOf('\n');
            int lastFence = content.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                return content.substring(firstNl + 1, lastFence).trim();
            }
        }
        return content;
    }
}

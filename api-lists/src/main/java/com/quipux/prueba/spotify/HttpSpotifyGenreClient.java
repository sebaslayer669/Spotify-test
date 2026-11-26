package com.quipux.prueba.spotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class HttpSpotifyGenreClient implements SpotifyGenreClient {

    private static final Logger log = LoggerFactory.getLogger(HttpSpotifyGenreClient.class);

    private final SpotifyProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public HttpSpotifyGenreClient(SpotifyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> listGenres() {
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            log.warn("SPOTIFY_CLIENT_ID no configurado; devolviendo lista vacía");
            return List.of();
        }
        try {
            String token = fetchToken();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCategoriesUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Spotify categories HTTP {}", response.statusCode());
                return List.of();
            }
            JsonNode items = objectMapper.readTree(response.body()).path("categories").path("items");
            List<String> genres = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    genres.add(item.path("name").asText());
                }
            }
            return genres;
        } catch (Exception e) {
            log.warn("Fallo Spotify genres: {}", e.getMessage());
            return List.of();
        }
    }

    private String fetchToken() throws Exception {
        String form = "grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8);
        String basic = Base64.getEncoder().encodeToString(
                (properties.getClientId() + ":" + properties.getClientSecret()).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getTokenUrl()))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Spotify token HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body()).path("access_token").asText();
    }
}

package com.quipux.prueba.ai;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.service.ListaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orquesta lista + IA + fallback + caché simple (control de costo/abuso).
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ListaService listaService;
    private final AiRecommendationClient aiRecommendationClient;
    private final AiProperties properties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RecommendationService(
            ListaService listaService,
            AiRecommendationClient aiRecommendationClient,
            AiProperties properties
    ) {
        this.listaService = listaService;
        this.aiRecommendationClient = aiRecommendationClient;
        this.properties = properties;
    }

    public RecommendationsResponse recommend(String listName) {
        ListaReproduccion lista = listaService.require(listName);

        CacheEntry cached = cache.get(listName);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.response();
        }

        try {
            List<RecommendationItem> items = aiRecommendationClient.recommend(lista);
            RecommendationsResponse ok = new RecommendationsResponse();
            ok.setListName(listName);
            ok.setSource("AI");
            ok.setMessage("Sugerencias listas");
            ok.setRecommendations(items);
            cache.put(listName, new CacheEntry(ok, Instant.now().plusSeconds(properties.getCacheTtlSeconds())));
            return ok;
        } catch (AiTimeoutException | AiProviderException ex) {
            // Fallback elegante: no tumba la aplicación.
            log.warn("IA no disponible para lista '{}': {}", listName, ex.getMessage());
            RecommendationsResponse fallback = defaultRecommendations(listName, lista);
            // No cachear fallback agresivamente; TTL corto implícito no guardado.
            return fallback;
        }
    }

    private RecommendationsResponse defaultRecommendations(String listName, ListaReproduccion lista) {
        String genero = lista.getCanciones().stream()
                .map(c -> c.getGenero())
                .filter(g -> g != null && !g.isBlank())
                .findFirst()
                .orElse("pop");

        RecommendationsResponse response = new RecommendationsResponse();
        response.setListName(listName);
        response.setSource("FALLBACK");
        response.setMessage("Sugerencias por defecto");
        response.setRecommendations(List.of(
                new RecommendationItem(
                        "Más " + genero,
                        "Selección local",
                        genero,
                        "Basado en el género de tu lista"
                ),
                new RecommendationItem(
                        "Descubrimiento " + genero,
                        "Varios artistas",
                        genero,
                        "Alternativa cercana a tus canciones"
                )
        ));
        return response;
    }

    private record CacheEntry(RecommendationsResponse response, Instant expiresAt) {
    }
}

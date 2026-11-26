package com.quipux.prueba.ai;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.service.ListaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta lista + Strategy ({@link AiRecommendationClient}) + caché + fallback.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ListaService listaService;
    private final AiRecommendationClient aiRecommendationClient;
    private final RecommendationCache cache;
    private final AiProperties properties;

    public RecommendationService(
            ListaService listaService,
            AiRecommendationClient aiRecommendationClient,
            RecommendationCache cache,
            AiProperties properties
    ) {
        this.listaService = listaService;
        this.aiRecommendationClient = aiRecommendationClient;
        this.cache = cache;
        this.properties = properties;
    }

    public RecommendationsResponse recommend(String listName) {
        ListaReproduccion lista = listaService.require(listName);

        return cache.getIfFresh(listName).orElseGet(() -> resolve(listName, lista));
    }

    private RecommendationsResponse resolve(String listName, ListaReproduccion lista) {
        try {
            List<RecommendationItem> items = aiRecommendationClient.recommend(lista);
            RecommendationsResponse ok = RecommendationsResponse.ai(listName, items);
            cache.put(listName, ok, properties.getCacheTtlSeconds());
            return ok;
        } catch (AiTimeoutException | AiProviderException ex) {
            log.warn("IA no disponible para lista '{}': {}", listName, ex.getMessage());
            return RecommendationsResponse.fallback(listName, dominantGenre(lista));
        }
    }

    private static String dominantGenre(ListaReproduccion lista) {
        return lista.getCanciones().stream()
                .map(c -> c.getGenero())
                .filter(g -> g != null && !g.isBlank())
                .findFirst()
                .orElse("pop");
    }
}

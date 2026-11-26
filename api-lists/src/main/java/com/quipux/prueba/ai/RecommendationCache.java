package com.quipux.prueba.ai;

import com.quipux.prueba.lists.event.ListDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria con TTL (control de costo/abuso).
 * Se invalida al borrar una lista (Observer vía eventos Spring).
 */
@Component
public class RecommendationCache {

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public Optional<RecommendationsResponse> getIfFresh(String listName) {
        CacheEntry entry = entries.get(listName);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now()) || entry.expiresAt().equals(Instant.now())) {
            entries.remove(listName);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    public void put(String listName, RecommendationsResponse response, long ttlSeconds) {
        entries.put(listName, new CacheEntry(response, Instant.now().plusSeconds(ttlSeconds)));
    }

    public void invalidate(String listName) {
        entries.remove(listName);
    }

    @EventListener
    public void onListDeleted(ListDeletedEvent event) {
        invalidate(event.listName());
    }

    private record CacheEntry(RecommendationsResponse response, Instant expiresAt) {
    }
}

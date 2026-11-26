package com.quipux.prueba.ai;

import com.quipux.prueba.lists.domain.Cancion;
import com.quipux.prueba.lists.domain.ListaReproduccion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cliente local cuando AI_ENABLED=false (desarrollo sin proveedor).
 * Devuelve sugerencias coherentes con el género de la playlist, sin mensajes técnicos.
 */
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "false")
public class FallbackAiRecommendationClient implements AiRecommendationClient {

    @Override
    public List<RecommendationItem> recommend(ListaReproduccion lista) {
        String genero = resolveGenre(lista);
        return suggestionsFor(genero);
    }

    private static String resolveGenre(ListaReproduccion lista) {
        if (lista.getCanciones() == null) {
            return "pop";
        }
        return lista.getCanciones().stream()
                .map(Cancion::getGenero)
                .filter(g -> g != null && !g.isBlank())
                .findFirst()
                .map(g -> g.trim().toLowerCase(Locale.ROOT))
                .orElse("pop");
    }

    private static List<RecommendationItem> suggestionsFor(String genero) {
        List<RecommendationItem> items = new ArrayList<>();
        switch (genero) {
            case "rock", "grunge", "metal" -> {
                items.add(new RecommendationItem("Everlong", "Foo Fighters", genero, "Misma energía de guitarra"));
                items.add(new RecommendationItem("Black Hole Sun", "Soundgarden", genero, "Atmósfera cercana a tu lista"));
                items.add(new RecommendationItem("Karma Police", "Radiohead", "rock", "Melodía y textura compatibles"));
            }
            case "pop" -> {
                items.add(new RecommendationItem("Blinding Lights", "The Weeknd", "pop", "Ritmo y producción actuales"));
                items.add(new RecommendationItem("Levitating", "Dua Lipa", "pop", "Melodía pegajosa afín"));
                items.add(new RecommendationItem("As It Was", "Harry Styles", "pop", "Pop contemporáneo"));
            }
            case "salsa", "latina", "latin" -> {
                items.add(new RecommendationItem("Idilio", "Willie González", "salsa", "Clásico del mismo universo"));
                items.add(new RecommendationItem("Lluvia", "Eddie Santiago", "salsa", "Estilo romántico compatible"));
                items.add(new RecommendationItem("Valió la Pena", "Marc Anthony", "salsa", "Energía similar"));
            }
            case "jazz" -> {
                items.add(new RecommendationItem("So What", "Miles Davis", "jazz", "Estándar esencial del género"));
                items.add(new RecommendationItem("Take Five", "Dave Brubeck", "jazz", "Ritmo y fraseo afines"));
                items.add(new RecommendationItem("Autumn Leaves", "Cannonball Adderley", "jazz", "Armonía clásica"));
            }
            default -> {
                items.add(new RecommendationItem("Dreams", "Fleetwood Mac", genero, "Melodía accesible cercana a tu lista"));
                items.add(new RecommendationItem("Mr. Brightside", "The Killers", genero, "Ritmo y gancho compatibles"));
                items.add(new RecommendationItem("Yellow", "Coldplay", genero, "Textura y atmósfera similares"));
            }
        }
        return items;
    }
}

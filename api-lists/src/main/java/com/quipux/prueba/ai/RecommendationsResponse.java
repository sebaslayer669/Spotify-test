package com.quipux.prueba.ai;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsResponse {

    private String listName;
    private String source;
    private String message;
    private List<RecommendationItem> recommendations = new ArrayList<>();

    public static RecommendationsResponse ai(String listName, List<RecommendationItem> items) {
        RecommendationsResponse response = new RecommendationsResponse();
        response.setListName(listName);
        response.setSource(RecommendationSource.AI.name());
        response.setMessage("Sugerencias listas");
        response.setRecommendations(items);
        return response;
    }

    public static RecommendationsResponse fallback(String listName, String genero) {
        RecommendationsResponse response = new RecommendationsResponse();
        response.setListName(listName);
        response.setSource(RecommendationSource.FALLBACK.name());
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

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RecommendationItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }
}

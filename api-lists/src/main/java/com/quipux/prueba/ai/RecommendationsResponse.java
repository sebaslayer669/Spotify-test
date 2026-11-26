package com.quipux.prueba.ai;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsResponse {

    private String listName;
    private String source; // AI | FALLBACK
    private String message;
    private List<RecommendationItem> recommendations = new ArrayList<>();

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

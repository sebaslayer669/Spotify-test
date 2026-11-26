package com.quipux.prueba.spotify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.spotify")
public class SpotifyProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String tokenUrl = "https://accounts.spotify.com/api/token";
    private String categoriesUrl = "https://api.spotify.com/v1/browse/categories";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getCategoriesUrl() {
        return categoriesUrl;
    }

    public void setCategoriesUrl(String categoriesUrl) {
        this.categoriesUrl = categoriesUrl;
    }
}

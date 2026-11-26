package com.quipux.prueba.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private long timeoutMs = 3000;
    private int maxPromptSongs = 20;
    /** Tope de tokens de respuesta del LLM (control de costo). */
    private int maxTokens = 500;
    /** Truncado por campo de canción / nombre en el prompt (prompt injection + costo). */
    private int maxFieldChars = 100;
    private long cacheTtlSeconds = 60;
    private String promptResource = "classpath:prompts/recommendations-v1.txt";
    private boolean enabled = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxPromptSongs() {
        return maxPromptSongs;
    }

    public void setMaxPromptSongs(int maxPromptSongs) {
        this.maxPromptSongs = maxPromptSongs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMaxFieldChars() {
        return maxFieldChars;
    }

    public void setMaxFieldChars(int maxFieldChars) {
        this.maxFieldChars = maxFieldChars;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getPromptResource() {
        return promptResource;
    }

    public void setPromptResource(String promptResource) {
        this.promptResource = promptResource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

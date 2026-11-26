package com.quipux.prueba.ai;

public class RecommendationItem {

    private String titulo;
    private String artista;
    private String genero;
    private String motivo;

    public RecommendationItem() {
    }

    public RecommendationItem(String titulo, String artista, String genero, String motivo) {
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.motivo = motivo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

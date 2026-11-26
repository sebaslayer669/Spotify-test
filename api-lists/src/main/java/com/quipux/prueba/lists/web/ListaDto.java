package com.quipux.prueba.lists.web;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class ListaDto {

    @NotBlank(message = "nombre es obligatorio")
    private String nombre;
    private String descripcion;
    private List<CancionDto> canciones = new ArrayList<>();

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<CancionDto> getCanciones() {
        return canciones;
    }

    public void setCanciones(List<CancionDto> canciones) {
        this.canciones = canciones != null ? canciones : new ArrayList<>();
    }

    public static class CancionDto {
        private String titulo;
        private String artista;
        private String album;
        private String anno;
        private String genero;

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

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getAnno() {
            return anno;
        }

        public void setAnno(String anno) {
            this.anno = anno;
        }

        public String getGenero() {
            return genero;
        }

        public void setGenero(String genero) {
            this.genero = genero;
        }
    }
}

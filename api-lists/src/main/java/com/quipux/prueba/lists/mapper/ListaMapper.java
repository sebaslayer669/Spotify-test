package com.quipux.prueba.lists.mapper;

import com.quipux.prueba.lists.domain.Cancion;
import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.web.ListaDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper DTO ↔ entidad (Single Responsibility: no mezcla persistencia ni reglas de negocio).
 */
@Component
public class ListaMapper {

    public ListaReproduccion toEntity(ListaDto dto) {
        ListaReproduccion entity = new ListaReproduccion();
        entity.setNombre(dto.getNombre());
        entity.setDescripcion(dto.getDescripcion());
        entity.setCanciones(toCanciones(dto.getCanciones()));
        return entity;
    }

    public ListaDto toDto(ListaReproduccion entity) {
        ListaDto dto = new ListaDto();
        dto.setNombre(entity.getNombre());
        dto.setDescripcion(entity.getDescripcion());
        dto.setCanciones(toCancionDtos(entity.getCanciones()));
        return dto;
    }

    public Cancion toCancion(ListaDto.CancionDto dto) {
        if (dto == null) {
            return new Cancion();
        }
        Cancion cancion = new Cancion();
        cancion.setTitulo(dto.getTitulo());
        cancion.setArtista(dto.getArtista());
        cancion.setAlbum(dto.getAlbum());
        cancion.setAnno(dto.getAnno());
        cancion.setGenero(dto.getGenero());
        return cancion;
    }

    private List<Cancion> toCanciones(List<ListaDto.CancionDto> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        List<Cancion> canciones = new ArrayList<>();
        for (ListaDto.CancionDto dto : source) {
            canciones.add(toCancion(dto));
        }
        return canciones;
    }

    private List<ListaDto.CancionDto> toCancionDtos(List<Cancion> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        List<ListaDto.CancionDto> dtos = new ArrayList<>();
        for (Cancion cancion : source) {
            dtos.add(toCancionDto(cancion));
        }
        return dtos;
    }

    private ListaDto.CancionDto toCancionDto(Cancion cancion) {
        ListaDto.CancionDto dto = new ListaDto.CancionDto();
        dto.setTitulo(cancion.getTitulo());
        dto.setArtista(cancion.getArtista());
        dto.setAlbum(cancion.getAlbum());
        dto.setAnno(cancion.getAnno());
        dto.setGenero(cancion.getGenero());
        return dto;
    }
}

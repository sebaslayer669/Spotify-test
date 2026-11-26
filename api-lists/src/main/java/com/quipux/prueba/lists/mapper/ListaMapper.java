package com.quipux.prueba.lists.mapper;

import com.quipux.prueba.lists.domain.Cancion;
import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.web.ListaDto;
import org.springframework.stereotype.Component;

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

    private List<Cancion> toCanciones(List<ListaDto.CancionDto> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(this::toCancion).toList();
    }

    private Cancion toCancion(ListaDto.CancionDto dto) {
        Cancion cancion = new Cancion();
        cancion.setTitulo(dto.getTitulo());
        cancion.setArtista(dto.getArtista());
        cancion.setAlbum(dto.getAlbum());
        cancion.setAnno(dto.getAnno());
        cancion.setGenero(dto.getGenero());
        return cancion;
    }

    private List<ListaDto.CancionDto> toCancionDtos(List<Cancion> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(this::toCancionDto).toList();
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

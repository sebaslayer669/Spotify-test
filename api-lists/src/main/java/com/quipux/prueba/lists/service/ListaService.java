package com.quipux.prueba.lists.service;

import com.quipux.prueba.lists.domain.Cancion;
import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.repo.ListaReproduccionRepository;
import com.quipux.prueba.lists.web.ListaDto;
import com.quipux.prueba.shared.exception.BadRequestException;
import com.quipux.prueba.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListaService {

    private final ListaReproduccionRepository repository;

    public ListaService(ListaReproduccionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ListaDto create(ListaDto dto) {
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new BadRequestException("El nombre de la lista no es válido");
        }
        if (repository.existsById(dto.getNombre())) {
            throw new BadRequestException("Ya existe una lista con ese nombre");
        }
        ListaReproduccion entity = toEntity(dto);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ListaDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ListaDto findByName(String listName) {
        return toDto(require(listName));
    }

    @Transactional
    public void delete(String listName) {
        if (!repository.existsById(listName)) {
            throw new NotFoundException("Lista no encontrada: " + listName);
        }
        repository.deleteById(listName);
    }

    @Transactional(readOnly = true)
    public ListaReproduccion require(String listName) {
        return repository.findById(listName)
                .orElseThrow(() -> new NotFoundException("Lista no encontrada: " + listName));
    }

    private ListaReproduccion toEntity(ListaDto dto) {
        ListaReproduccion entity = new ListaReproduccion();
        entity.setNombre(dto.getNombre());
        entity.setDescripcion(dto.getDescripcion());
        entity.setCanciones(dto.getCanciones().stream().map(c -> {
            Cancion cancion = new Cancion();
            cancion.setTitulo(c.getTitulo());
            cancion.setArtista(c.getArtista());
            cancion.setAlbum(c.getAlbum());
            cancion.setAnno(c.getAnno());
            cancion.setGenero(c.getGenero());
            return cancion;
        }).toList());
        return entity;
    }

    private ListaDto toDto(ListaReproduccion entity) {
        ListaDto dto = new ListaDto();
        dto.setNombre(entity.getNombre());
        dto.setDescripcion(entity.getDescripcion());
        dto.setCanciones(entity.getCanciones().stream().map(c -> {
            ListaDto.CancionDto cd = new ListaDto.CancionDto();
            cd.setTitulo(c.getTitulo());
            cd.setArtista(c.getArtista());
            cd.setAlbum(c.getAlbum());
            cd.setAnno(c.getAnno());
            cd.setGenero(c.getGenero());
            return cd;
        }).toList());
        return dto;
    }
}

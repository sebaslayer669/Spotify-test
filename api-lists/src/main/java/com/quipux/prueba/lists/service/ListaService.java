package com.quipux.prueba.lists.service;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.event.ListDeletedEvent;
import com.quipux.prueba.lists.event.ListUpdatedEvent;
import com.quipux.prueba.lists.mapper.ListaMapper;
import com.quipux.prueba.lists.repo.ListaReproduccionRepository;
import com.quipux.prueba.lists.web.ListaDto;
import com.quipux.prueba.shared.exception.BadRequestException;
import com.quipux.prueba.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListaService {

    private final ListaReproduccionRepository repository;
    private final ListaMapper mapper;
    private final ApplicationEventPublisher events;

    public ListaService(
            ListaReproduccionRepository repository,
            ListaMapper mapper,
            ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional
    public ListaDto create(ListaDto dto) {
        validateNombre(dto.getNombre());
        if (repository.existsById(dto.getNombre())) {
            throw new BadRequestException("Ya existe una lista con ese nombre");
        }
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<ListaDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ListaDto findByName(String listName) {
        return mapper.toDto(require(listName));
    }

    @Transactional
    public ListaDto addCancion(String listName, ListaDto.CancionDto cancionDto) {
        requireValidCancion(cancionDto);
        ListaReproduccion entity = require(listName);
        entity.getCanciones().add(mapper.toCancion(cancionDto));
        return persistUpdated(listName, entity);
    }

    @Transactional
    public ListaDto updateCancion(String listName, int index, ListaDto.CancionDto cancionDto) {
        requireValidCancion(cancionDto);
        if (index < 0) {
            throw new BadRequestException("Índice de canción inválido");
        }
        ListaReproduccion entity = require(listName);
        if (index >= entity.getCanciones().size()) {
            throw new NotFoundException("Canción no encontrada en la lista");
        }
        entity.getCanciones().set(index, mapper.toCancion(cancionDto));
        return persistUpdated(listName, entity);
    }

    @Transactional
    public void delete(String listName) {
        if (!repository.existsById(listName)) {
            throw new NotFoundException("Lista no encontrada: " + listName);
        }
        repository.deleteById(listName);
        events.publishEvent(new ListDeletedEvent(listName));
    }

    @Transactional(readOnly = true)
    public ListaReproduccion require(String listName) {
        return repository.findById(listName)
                .orElseThrow(() -> new NotFoundException("Lista no encontrada: " + listName));
    }

    private ListaDto persistUpdated(String listName, ListaReproduccion entity) {
        ListaDto updated = mapper.toDto(repository.save(entity));
        events.publishEvent(new ListUpdatedEvent(listName));
        return updated;
    }

    private static void requireValidCancion(ListaDto.CancionDto cancionDto) {
        if (cancionDto == null) {
            throw new BadRequestException("La canción no es válida");
        }
        boolean empty =
                isBlank(cancionDto.getTitulo())
                        && isBlank(cancionDto.getArtista())
                        && isBlank(cancionDto.getAlbum())
                        && isBlank(cancionDto.getAnno())
                        && isBlank(cancionDto.getGenero());
        if (empty) {
            throw new BadRequestException("Indica al menos título o artista");
        }
    }

    private static void validateNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BadRequestException("El nombre de la lista no es válido");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

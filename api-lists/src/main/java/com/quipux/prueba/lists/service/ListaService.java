package com.quipux.prueba.lists.service;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.event.ListDeletedEvent;
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

    private static void validateNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BadRequestException("El nombre de la lista no es válido");
        }
    }
}

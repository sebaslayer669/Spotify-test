package com.quipux.prueba.lists;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.event.ListDeletedEvent;
import com.quipux.prueba.lists.mapper.ListaMapper;
import com.quipux.prueba.lists.repo.ListaReproduccionRepository;
import com.quipux.prueba.lists.service.ListaService;
import com.quipux.prueba.lists.web.ListaDto;
import com.quipux.prueba.shared.exception.BadRequestException;
import com.quipux.prueba.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaServiceTest {

    @Mock
    private ListaReproduccionRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    private ListaService listaService;

    @BeforeEach
    void setUp() {
        listaService = new ListaService(repository, new ListaMapper(), events);
    }

    @Test
    void createPersistsListaWithCanciones() {
        ListaDto dto = sampleDto("Rock");
        when(repository.existsById("Rock")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ListaDto created = listaService.create(dto);

        assertThat(created.getNombre()).isEqualTo("Rock");
        assertThat(created.getCanciones()).hasSize(1);
        assertThat(created.getCanciones().get(0).getTitulo()).isEqualTo("Song");

        ArgumentCaptor<ListaReproduccion> captor = ArgumentCaptor.forClass(ListaReproduccion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Desc");
    }

    @Test
    void createRejectsNullOrBlankNombre() {
        ListaDto nullName = new ListaDto();
        nullName.setNombre(null);
        assertThatThrownBy(() -> listaService.create(nullName))
                .isInstanceOf(BadRequestException.class);

        ListaDto blank = new ListaDto();
        blank.setNombre("  ");
        assertThatThrownBy(() -> listaService.create(blank))
                .isInstanceOf(BadRequestException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNombre() {
        ListaDto dto = sampleDto("Rock");
        when(repository.existsById("Rock")).thenReturn(true);

        assertThatThrownBy(() -> listaService.create(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void findByNameReturnsLista() {
        ListaReproduccion entity = new ListaReproduccion();
        entity.setNombre("Jazz");
        entity.setDescripcion("Cool");
        when(repository.findById("Jazz")).thenReturn(Optional.of(entity));

        ListaDto found = listaService.findByName("Jazz");

        assertThat(found.getNombre()).isEqualTo("Jazz");
        assertThat(found.getDescripcion()).isEqualTo("Cool");
    }

    @Test
    void findByNameThrowsWhenMissing() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listaService.findByName("x"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void findAllMapsEntities() {
        ListaReproduccion a = new ListaReproduccion();
        a.setNombre("A");
        ListaReproduccion b = new ListaReproduccion();
        b.setNombre("B");
        when(repository.findAll()).thenReturn(List.of(a, b));

        assertThat(listaService.findAll()).extracting(ListaDto::getNombre).containsExactly("A", "B");
    }

    @Test
    void deleteRemovesExistingListaAndPublishesEvent() {
        when(repository.existsById("Rock")).thenReturn(true);

        listaService.delete("Rock");

        verify(repository).deleteById("Rock");
        ArgumentCaptor<ListDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ListDeletedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().listName()).isEqualTo("Rock");
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(repository.existsById("x")).thenReturn(false);

        assertThatThrownBy(() -> listaService.delete("x"))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).deleteById(any());
        verify(events, never()).publishEvent(any());
    }

    private static ListaDto sampleDto(String nombre) {
        ListaDto dto = new ListaDto();
        dto.setNombre(nombre);
        dto.setDescripcion("Desc");
        ListaDto.CancionDto song = new ListaDto.CancionDto();
        song.setTitulo("Song");
        song.setArtista("Artist");
        song.setAlbum("Album");
        song.setAnno("2000");
        song.setGenero("rock");
        dto.setCanciones(List.of(song));
        return dto;
    }
}

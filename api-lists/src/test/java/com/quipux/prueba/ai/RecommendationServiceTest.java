package com.quipux.prueba.ai;

import com.quipux.prueba.lists.domain.Cancion;
import com.quipux.prueba.lists.domain.ListaReproduccion;
import com.quipux.prueba.lists.service.ListaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ListaService listaService;

    @Mock
    private AiRecommendationClient aiRecommendationClient;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setCacheTtlSeconds(60);
        recommendationService = new RecommendationService(listaService, aiRecommendationClient, properties);
    }

    @Test
    void returnsAiRecommendationsWhenProviderOk() {
        ListaReproduccion lista = sampleLista();
        when(listaService.require("rock")).thenReturn(lista);
        when(aiRecommendationClient.recommend(any())).thenReturn(List.of(
                new RecommendationItem("Song A", "Artist A", "rock", "similar")
        ));

        RecommendationsResponse response = recommendationService.recommend("rock");

        assertThat(response.getSource()).isEqualTo("AI");
        assertThat(response.getRecommendations()).hasSize(1);
        assertThat(response.getRecommendations().get(0).getTitulo()).isEqualTo("Song A");
    }

    @Test
    void fallsBackWhenProviderTimesOut() {
        ListaReproduccion lista = sampleLista();
        when(listaService.require("rock")).thenReturn(lista);
        when(aiRecommendationClient.recommend(any()))
                .thenThrow(new AiTimeoutException("timeout simulado"));

        RecommendationsResponse response = recommendationService.recommend("rock");

        assertThat(response.getSource()).isEqualTo("FALLBACK");
        assertThat(response.getRecommendations()).isNotEmpty();
        assertThat(response.getRecommendations().get(0).getGenero()).isEqualToIgnoringCase("rock");
    }

    @Test
    void fallsBackWhenProviderThrows() {
        ListaReproduccion lista = sampleLista();
        when(listaService.require("rock")).thenReturn(lista);
        when(aiRecommendationClient.recommend(any()))
                .thenThrow(new AiProviderException("basura del modelo"));

        RecommendationsResponse response = recommendationService.recommend("rock");

        assertThat(response.getSource()).isEqualTo("FALLBACK");
    }

    private ListaReproduccion sampleLista() {
        ListaReproduccion lista = new ListaReproduccion();
        lista.setNombre("rock");
        Cancion c = new Cancion();
        c.setTitulo("Bohemian Rhapsody");
        c.setArtista("Queen");
        c.setGenero("rock");
        lista.setCanciones(List.of(c));
        return lista;
    }
}

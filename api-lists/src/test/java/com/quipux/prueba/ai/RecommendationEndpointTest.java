package com.quipux.prueba.ai;

import com.quipux.prueba.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AiRecommendationClient aiRecommendationClient;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = "Bearer " + jwtService.generateToken("demo");
    }

    @Test
    void recommendationsWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/lists/rock/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recommendationsForMissingListReturns404() throws Exception {
        mockMvc.perform(get("/lists/no-existe/recommendations")
                        .header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommendationsReturns200WhenAiOk() throws Exception {
        createLista("Lista IT OK");
        when(aiRecommendationClient.recommend(any())).thenReturn(List.of(
                new RecommendationItem("Song IT", "Artist IT", "rock", "similar")
        ));

        mockMvc.perform(get("/lists/Lista IT OK/recommendations")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.recommendations[0].titulo").value("Song IT"));
    }

    @Test
    void recommendationsFallsBackWhenAiTimesOut() throws Exception {
        createLista("Lista IT Timeout");
        when(aiRecommendationClient.recommend(any()))
                .thenThrow(new AiTimeoutException("timeout IT"));

        mockMvc.perform(get("/lists/Lista IT Timeout/recommendations")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("FALLBACK"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations").isNotEmpty());
    }

    @Test
    void recommendationsFallsBackWhenAiProviderFails() throws Exception {
        createLista("Lista IT Provider");
        when(aiRecommendationClient.recommend(any()))
                .thenThrow(new AiProviderException("basura IT"));

        mockMvc.perform(get("/lists/Lista IT Provider/recommendations")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("FALLBACK"));
    }

    private void createLista(String nombre) throws Exception {
        String body = """
                {
                  "nombre": "%s",
                  "descripcion": "IT",
                  "canciones": [
                    {"titulo": "Bohemian Rhapsody", "artista": "Queen", "album": "A Night", "anno": "1975", "genero": "rock"}
                  ]
                }
                """.formatted(nombre);

        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}

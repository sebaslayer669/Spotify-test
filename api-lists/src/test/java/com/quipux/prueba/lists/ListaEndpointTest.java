package com.quipux.prueba.lists;

import com.quipux.prueba.ai.AiRecommendationClient;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListaEndpointTest {

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
    void postListsWithoutJwtReturns401() throws Exception {
        mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("SinToken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postListsCreates201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Lista SENIOR")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/lists/Lista%20SENIOR")))
                .andExpect(jsonPath("$.nombre").value("Lista SENIOR"))
                .andExpect(jsonPath("$.descripcion").value("Lista de canciones de Spotify"))
                .andExpect(jsonPath("$.canciones", hasSize(1)))
                .andExpect(jsonPath("$.canciones[0].titulo").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.canciones[0].artista").value("Queen"))
                .andExpect(jsonPath("$.canciones[0].album").value("A Night at the Opera"))
                .andExpect(jsonPath("$.canciones[0].anno").value("1975"))
                .andExpect(jsonPath("$.canciones[0].genero").value("rock"));
    }

    @Test
    void postListsWithBlankNombreReturns400() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","descripcion":"x","canciones":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postListsWithNullNombreReturns400() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":null,"descripcion":"x","canciones":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListsReturnsArray() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("ParaListar")));

        mockMvc.perform(get("/lists").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getListByNameReturns200() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Detalle")));

        mockMvc.perform(get("/lists/Detalle").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Detalle"))
                .andExpect(jsonPath("$.canciones[0].genero").value("rock"));
    }

    @Test
    void getListByNameMissingReturns404() throws Exception {
        mockMvc.perform(get("/lists/no-existe-senior").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void postCancionAppendsToExistingList() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("ConCanciones")));

        mockMvc.perform(post("/lists/ConCanciones/canciones")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Another One Bites the Dust",
                                  "artista": "Queen",
                                  "album": "The Game",
                                  "anno": "1980",
                                  "genero": "rock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canciones", hasSize(2)))
                .andExpect(jsonPath("$.canciones[0].titulo").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.canciones[1].titulo").value("Another One Bites the Dust"));
    }

    @Test
    void putListaUpdatesDescripcion() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("MetaLista")));

        mockMvc.perform(put("/lists/MetaLista")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "MetaLista",
                                  "descripcion": "Descripción editada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Descripción editada"))
                .andExpect(jsonPath("$.canciones", hasSize(1)));
    }

    @Test
    void deleteCancionRemovesSongAtIndex() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("BorrarCancion")));

        mockMvc.perform(post("/lists/BorrarCancion/canciones")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Extra",
                                  "artista": "Band",
                                  "album": "",
                                  "anno": "",
                                  "genero": "rock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canciones", hasSize(2)));

        mockMvc.perform(delete("/lists/BorrarCancion/canciones/0")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canciones", hasSize(1)))
                .andExpect(jsonPath("$.canciones[0].titulo").value("Extra"));
    }

    @Test
    void putCancionUpdatesSongAtIndex() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("EditarCancion")));

        mockMvc.perform(put("/lists/EditarCancion/canciones/0")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Don't Stop Me Now",
                                  "artista": "Queen",
                                  "album": "Jazz",
                                  "anno": "1978",
                                  "genero": "rock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canciones", hasSize(1)))
                .andExpect(jsonPath("$.canciones[0].titulo").value("Don't Stop Me Now"))
                .andExpect(jsonPath("$.canciones[0].album").value("Jazz"));
    }

    @Test
    void deleteListReturns204() throws Exception {
        mockMvc.perform(post("/lists")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("BorrarOK")));

        mockMvc.perform(delete("/lists/BorrarOK").header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/lists/BorrarOK").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingListReturns404() throws Exception {
        mockMvc.perform(delete("/lists/no-existe-delete").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    private static String validBody(String nombre) {
        return """
                {
                  "nombre": "%s",
                  "descripcion": "Lista de canciones de Spotify",
                  "canciones": [
                    {
                      "titulo": "Bohemian Rhapsody",
                      "artista": "Queen",
                      "album": "A Night at the Opera",
                      "anno": "1975",
                      "genero": "rock"
                    }
                  ]
                }
                """.formatted(nombre);
    }
}

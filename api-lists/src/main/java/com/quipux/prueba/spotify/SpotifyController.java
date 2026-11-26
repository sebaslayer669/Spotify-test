package com.quipux.prueba.spotify;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spotify")
public class SpotifyController {

    private final SpotifyGenreClient spotifyGenreClient;

    public SpotifyController(SpotifyGenreClient spotifyGenreClient) {
        this.spotifyGenreClient = spotifyGenreClient;
    }

    @GetMapping("/genres")
    public Map<String, List<String>> genres() {
        return Map.of("genres", spotifyGenreClient.listGenres());
    }
}

package com.quipux.prueba.lists.web;

import com.quipux.prueba.lists.service.ListaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/lists")
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @PostMapping
    public ResponseEntity<ListaDto> create(@Valid @RequestBody ListaDto body) {
        ListaDto created = listaService.create(body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{listName}")
                .buildAndExpand(created.getNombre())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ListaDto> findAll() {
        return listaService.findAll();
    }

    @GetMapping("/{listName}")
    public ListaDto findByName(@PathVariable String listName) {
        return listaService.findByName(listName);
    }

    @DeleteMapping("/{listName}")
    public ResponseEntity<Void> delete(@PathVariable String listName) {
        listaService.delete(listName);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

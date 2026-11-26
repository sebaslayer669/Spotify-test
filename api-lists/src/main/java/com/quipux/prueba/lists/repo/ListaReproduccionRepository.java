package com.quipux.prueba.lists.repo;

import com.quipux.prueba.lists.domain.ListaReproduccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaReproduccionRepository extends JpaRepository<ListaReproduccion, String> {
}

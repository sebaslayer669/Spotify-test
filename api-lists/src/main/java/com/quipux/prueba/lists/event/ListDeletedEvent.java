package com.quipux.prueba.lists.event;

/**
 * Evento de dominio: permite desacoplar borrado de lista de efectos secundarios (caché IA, etc.).
 */
public record ListDeletedEvent(String listName) {
}

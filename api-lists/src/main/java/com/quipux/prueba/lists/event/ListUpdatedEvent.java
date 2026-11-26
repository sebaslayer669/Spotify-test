package com.quipux.prueba.lists.event;

/**
 * Publicado cuando cambia el contenido de una lista (p. ej. se agrega una canción).
 */
public record ListUpdatedEvent(String listName) {
}

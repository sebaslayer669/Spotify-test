package com.quipux.prueba.ai;

import com.quipux.prueba.lists.domain.ListaReproduccion;

import java.util.List;

/**
 * Contrato del proveedor de IA. Permite mockear timeouts/fallos en tests
 * y cambiar de proveedor (OpenAI, Gemini, Ollama) sin tocar el controller.
 */
public interface AiRecommendationClient {

    List<RecommendationItem> recommend(ListaReproduccion lista);
}

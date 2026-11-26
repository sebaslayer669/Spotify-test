# AI-LOG — Bitácora de uso de Inteligencia Artificial

Instrucciones: Completa esta bitácora mientras resuelves el Módulo de IA. Es un entregable obligatorio. Sé honesto y concreto. Copia los prompts tal cual los usaste. No hay respuestas "incorrectas" por usar IA — lo que evaluamos es tu criterio.

**Candidato:** _____________________  
**Nivel:** [ ] Staff I/II [ ] Staff III [x] Senior  
**Fecha:** 2026-11-25  
**Asistente(s) de IA usado(s):** Cursor (Composer / equipo de agentes)

> Catálogo de prompts del equipo: [`docs/PROMPTS-Y-EQUIPO.md`](docs/PROMPTS-Y-EQUIPO.md). Abajo solo los prompts **efectivamente usados**.

---

## Ejercicio 1

### Prompt(s) usado(s):

```text
Actúa como Backend Engineer Senior. Implementa:

GET /lists/{listName}/recommendations

Comportamiento:
- Si la lista no existe → 404.
- Protegido por JWT.
- Usa un servicio de IA (LLM vía API) para sugerir canciones/géneros según las canciones de la lista.
- El prompt del modelo debe estar versionado en el repo (archivo o classpath resource).
- API key del proveedor SOLO por variable de entorno (AI_API_KEY).
- Timeout configurable (AI_TIMEOUT_MS).
- Si la IA falla/timeout/basura → fallback elegante; la app NO debe caerse.
- Control de costo/abuso: caché por listName, límite de canciones en el prompt.

Arquitectura:
- interface AiRecommendationClient
- implementación HTTP + Fake/Mock para tests
- RecommendationService que orquesta lista + IA + fallback
```

### Respuesta / código relevante de la IA:

Scaffold generado en `api-lists/`:

- `AiRecommendationClient` + `HttpAiRecommendationClient` + `FallbackAiRecommendationClient`
- `RecommendationService` (caché + fallback)
- `RecommendationController`
- Prompt versionado: `src/main/resources/prompts/recommendations-v1.txt`
- Secrets vía `application.yml` → `${AI_API_KEY:}` / variables de entorno

### ¿Qué acepté y por qué?

- Interfaz + implementación HTTP: permite mockear el proveedor (requisito Senior y Ejercicio 3).
- Prompt en classpath versionado (`recommendations-v1.txt`).
- Fallback a recomendación default con `source=FALLBACK` en lugar de tumbar la app.
- Caché TTL + `max-prompt-songs` como control de costo/abuso simple.

### ¿Qué rechacé o corregí y por qué?

- No hardcodear API keys ni tokens Spotify.
- No devolver 500 no controlado cuando el LLM falla: se degrada con mensaje claro.
- Login JWT simplificado (`/auth/login`) solo para la prueba; en producción iría contra usuarios reales.

### Explicación con mis palabras (¿entiendo lo que entrego?):

El endpoint carga la lista, arma un prompt versionado con un subconjunto de canciones, llama al LLM con timeout y API key por env. Si el proveedor falla, responde recomendaciones locales marcadas como FALLBACK. Los tests mockean `AiRecommendationClient` para probar timeout/excepción sin costo real.

---

## Ejercicio 2

### Prompt(s) usado(s):

**Generación del cliente (para revisar):**

```text
Genera la implementación COMPLETA en Java/Spring Boot de un cliente que consume un LLM para recomendaciones musicales a partir de una playlist.

Requisitos:
- RestClient o WebClient / HttpClient
- Timeout
- Lectura de API key desde environment
- Prompt cargado desde resource classpath (prompts/recommendations-v1.txt)
- Parseo de JSON a List<RecommendationItem>
- Excepciones tipadas (AiTimeoutException, AiProviderException)
- Logging sin imprimir la API key
```

**Revisión crítica Senior:**

```text
Actúa como Senior Security + Backend Reviewer. Revisa el cliente IA.
Checklist: secrets/logs, timeout real, parseo de basura/markdown, prompt injection,
testeabilidad (interfaz+mock), fallback/límites de tokens.
Para cada hallazgo: severidad, evidencia, fix concreto.
```

Código relevante generado (fragmento):

```java
public List<RecommendationItem> recommend(ListaReproduccion lista) {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
        throw new AiProviderException("AI_API_KEY no configurada");
    }
    String prompt = buildPrompt(lista);
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(trimSlash(properties.getBaseUrl()) + "/chat/completions"))
            .timeout(Duration.ofMillis(properties.getTimeoutMs()))
            .header("Authorization", "Bearer " + properties.getApiKey())
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    // parseo de choices[0].message.content → List<RecommendationItem>
}
```

Archivo: `api-lists/src/main/java/com/quipux/prueba/ai/HttpAiRecommendationClient.java`.

### Análisis / casos faltantes / hallazgos:

| Severidad | Hallazgo | Fix |
|-----------|----------|-----|
| High | Respuesta del modelo con fences markdown | `stripMarkdownFence` / `AiResponseParser` |
| High | Sin API key no debe tumbar el proceso | `AiProviderException` + fallback en service |
| High | Prompt injection vía canciones / nombre | `sanitizeField` + system prompt reforzado |
| Medium | Sin límite de canciones / tokens | `AI_MAX_PROMPT_SONGS` + `AI_MAX_TOKENS` |
| Medium | Llamadas repetidas al mismo listName | caché TTL |
| Medium | `recommendations: []` como éxito | excepción → fallback |
| Medium | Campos de canción ilimitados | `AI_MAX_FIELD_CHARS` |
| Low | Login demo sin password | documentado (simplificación de prueba) |

Checklist Senior: secrets OK, timeout real OK, parseo defensivo OK, injection mitigada, fallback + límites OK, testeable OK.

Pendientes menores (no bloquean): H5 invalidar caché al borrar; H6 default `AI_ENABLED`.

### Lo que implementé o corregí yo mismo:

- Interfaz `AiRecommendationClient` + beans condicionales (`AI_ENABLED`).
- Fallback en **service** (no solo en `@ExceptionHandler`).
- Caché TTL + límite de canciones + `max_tokens` + truncado/sanitizado (H1–H4).
- Limpieza de markdown y rechazo de array vacío.
- Tests de timeout / provider failure.

---

## Ejercicio 3

### Prompt(s) usado(s):

```text
Actúa como QA Engineer Senior (JUnit 5 + Mockito).
Genera tests para GET /lists/{listName}/recommendations:
1) 401 sin JWT (integración)
2) 404 lista inexistente
3) 200 cuando AiRecommendationClient OK
4) Fallback cuando AiTimeoutException
5) Fallback cuando AiProviderException
Usa mocks del cliente IA. No llames al proveedor real.
```

### Hallazgos / revisión crítica / riesgos detectados:

**Casos que generó / implementó la IA:**

| Caso | Dónde | Estado |
|------|--------|--------|
| 200 AI OK | `RecommendationServiceTest` + `RecommendationEndpointTest` | OK |
| Timeout → FALLBACK | ambos | OK |
| Provider exception → FALLBACK | ambos | OK |
| 401 sin JWT | MockMvc | OK |
| 404 lista inexistente | MockMvc | OK |

**Casos que añadí / corregí yo:**

- `AiResponseParser` + tests (markdown, vacío, JSON basura).
- Renombrar `*IT` → `RecommendationEndpointTest` (Surefire excluye `*IT`).
- `application-test.yml` + `@MockBean` (sin proveedor real).
- Test de `sanitizeField`.

**Riesgo residual:** sin test explícito de TTL de caché (no bloquea el módulo).

### Decisiones tomadas:

- Unit tests del service (baratos) + MockMvc con JWT real (`JwtService`).
- Nunca llamar al proveedor real en tests.
- Parser aislado para probar basura del LLM sin HTTP.
- Entregables del módulo (endpoint JWT, cliente IA, tests, AI-LOG, README Autoría) → OK (14 tests).

---

## Reflexión final (obligatoria)

### ¿Dónde falló, alucinó o se equivocó la IA durante esta prueba?

- Asumió JSON perfecto del LLM; hubo que limpiar markdown y content vacío.
- No contempló prompt injection ni `max_tokens` ni truncado de campos (aplicado en revisión Senior).
- Propuso o sugería secrets/tokens en ejemplos: se rechazó.
- Nombró tests `*IT` sin advertir que Surefire no los ejecuta.

### ¿Qué parte del trabajo NO delegarías a la IA y por qué?

- Decisiones de seguridad (dónde viven las keys, qué se loguea, política de fallback).
- Criterio de aceptación Senior y la revisión crítica del código generado (Ejercicio 2).
- Validación manual de que el endpoint no tumba la app ante fallo del proveedor.
- Priorizar qué casos de test son obligatorios vs. opcionales.

### Autoría:

Indica de forma general qué porcentaje/partes fueron asistidas por IA vs. escritas a mano.

| Parte | IA | Mano / criterio humano |
|-------|----|------------------------|
| Arquitectura / diseño | ~70% | ~30% (trade-offs fallback/caché) |
| Equipo + prompts (docs) | ~90% | ~10% (ajuste a enunciado Quipux) |
| API `/lists` + JWT + Spotify | ~80% | ~20% |
| Cliente IA + recommendations | ~70% | ~30% (resiliencia/seguridad) |
| Tests | ~65% | ~35% (MockMvc JWT, parser, Surefire `*IT`) |
| Front (Vue / consola) | ~75% | ~25% (contrato API, sin keys en browser) |
| Documentación (AI-LOG/README) | ~50% | ~50% (honestidad autoría + hallazgos) |

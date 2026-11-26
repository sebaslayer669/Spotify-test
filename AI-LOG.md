# AI-LOG — Bitácora de uso de Inteligencia Artificial

Instrucciones: Completa esta bitácora mientras resuelves el Módulo de IA. Es un entregable obligatorio. Sé honesto y concreto. Copia los prompts tal cual los usaste. No hay respuestas "incorrectas" por usar IA — lo que evaluamos es tu criterio.

| Campo | Valor |
|-------|--------|
| Candidato | _(completar nombre)_ |
| Nivel | Senior |
| Fecha | 2026-03-25 |
| Asistente(s) de IA usado(s) | Cursor (Composer / equipo de agentes) |

> Los prompts maestros del equipo están versionados en [`docs/PROMPTS-Y-EQUIPO.md`](docs/PROMPTS-Y-EQUIPO.md). Aquí se pegan los prompts **efectivamente usados** en cada ejercicio.

---

## Ejercicio 1 — Endpoint con IA integrada

### Prompt(s) usado(s)

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

Catálogo completo del equipo: `docs/PROMPTS-Y-EQUIPO.md` (§3.1–3.10).

### Respuesta / código relevante de la IA

Scaffold generado en `api-lists/`:
- `AiRecommendationClient` + `HttpAiRecommendationClient` + `FallbackAiRecommendationClient`
- `RecommendationService` (caché + fallback)
- `RecommendationController`
- Prompt versionado: `src/main/resources/prompts/recommendations-v1.txt`
- Secrets vía `application.yml` → `${AI_API_KEY}` / env

### ¿Qué acepté y por qué?

- Interfaz + implementación HTTP: permite mockear el proveedor (requisito Senior y Ejercicio 3).
- Prompt en classpath versionado (`recommendations-v1.txt`).
- Fallback a recomendación default con `source=FALLBACK` en lugar de tumbar la app.
- Caché TTL + `max-prompt-songs` como control de costo/abuso simple.

### ¿Qué rechacé o corregí y por qué?

- No hardcodear API keys ni tokens Spotify.
- No devolver 500 no controlado cuando el LLM falla: se degrada con mensaje claro.
- Login JWT simplificado (`/auth/login`) solo para la prueba; en producción iría contra usuarios reales.

### Explicación con mis palabras (¿entiendo lo que entrego?)

El endpoint carga la lista, arma un prompt versionado con un subconjunto de canciones, llama al LLM con timeout y API key por env. Si el proveedor falla, responde recomendaciones locales marcadas como FALLBACK. Los tests mockean `AiRecommendationClient` para probar timeout/excepción sin costo real.

---

## Ejercicio 2 — Revisión crítica de código generado por IA

> Según `Modulo-IA-Senior`: (1) pegar prompt y código generado, (2) revisión Senior con problemas relevantes, (3) describir cambios aplicados sobre la propuesta de la IA.

### Prompt(s) usado(s)

**1) Generación del cliente (para revisar):**

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

**2) Revisión crítica Senior:**

```text
Actúa como Senior Security + Backend Reviewer. Revisa el cliente IA.
Checklist: secrets/logs, timeout real, parseo de basura/markdown, prompt injection,
testeabilidad (interfaz+mock), fallback/límites de tokens.
Para cada hallazgo: severidad, evidencia, fix concreto.
```

### Respuesta / código generado por la IA (relevante)

Propuesta inicial generada (cliente HTTP + parseo). Archivo actual: `api-lists/src/main/java/com/quipux/prueba/ai/HttpAiRecommendationClient.java`.

```java
// Fragmento representativo de lo generado / propuesto por IA
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
    // ... parseo de choices[0].message.content a List<RecommendationItem>
}
```

También generó/orquestó: `AiRecommendationClient`, `RecommendationService` (fallback + caché), `PromptTemplateLoader`, prompt `recommendations-v1.txt`.

### Análisis / casos faltantes / hallazgos

**Checklist Senior (pasada sobre el código actual):**

| # | Criterio | Resultado |
|---|----------|-----------|
| 1 | Secrets / logs con API key | OK — key solo por env; no se loguea |
| 2 | Timeout real | OK — `HttpRequest.timeout` + connect timeout |
| 3 | Parseo de basura / markdown | OK — `stripMarkdownFence` + array vacío → excepción → fallback |
| 4 | Prompt injection (datos de usuario) | Mitigado — `sanitizeField` + system/prompt versionado |
| 5 | Fallback / límites de tokens | OK — fallback + `max_tokens` (`AI_MAX_TOKENS`) |
| 6 | Testeabilidad | OK — service mock + parser (`AiResponseParserTest`) + MockMvc (`RecommendationEndpointTest`) |

**Hallazgos ya corregidos sobre la propuesta de la IA:**

| Severidad | Hallazgo | Fix aplicado |
|-----------|----------|--------------|
| High | Respuesta del modelo puede venir con fences markdown | `stripMarkdownFence` en el parser |
| High | Sin API key no debe tumbar el proceso | `AiProviderException` + fallback en `RecommendationService` |
| High (H1) | Prompt injection vía canciones / nombre de lista | `sanitizeField` (saltos de línea/controles + truncado) + system prompt reforzado + regla en `recommendations-v1.txt` |
| Medium | Prompt sin límite de canciones → costo/abuso | `AI_MAX_PROMPT_SONGS` |
| Medium | Llamadas repetidas al mismo `listName` | caché TTL en `RecommendationService` |
| Medium (H2) | Sin tope de tokens de respuesta | `max_tokens` en request + `AI_MAX_TOKENS` / `app.ai.max-tokens` |
| Medium (H3) | `recommendations: []` tratado como éxito | `AiProviderException` → fallback del service |
| Medium (H4) | Campos de canción ilimitados en el prompt | Truncado por campo (`AI_MAX_FIELD_CHARS`, default 100) |
| Low | Login demo sin password | documentado como simplificación de prueba |

**Hallazgos abiertos (fuera del núcleo Ej. 2/3):**

| ID | Severidad | Hallazgo | Fix propuesto |
|----|-----------|----------|---------------|
| H5 | Medium | Caché solo por `listName` (stale / abuso con muchos nombres) | Invalidar al borrar; documentar rate limit / TTL |
| H6 | Low | `AI_ENABLED` default `true` sin key → siempre fallback | Default local `false` o documentar en README |

**Cerrado en Ejercicio 3:** H7 — tests del parser (markdown / vacío / JSON roto) en `AiResponseParserTest`.

### Lo que implementé o corregí yo mismo

- Interfaz `AiRecommendationClient` + beans condicionales (`AI_ENABLED` / `FallbackAiRecommendationClient`).
- Fallback en **service** (degradación elegante), no solo en `@ExceptionHandler`.
- Caché TTL + límite de canciones en el prompt (control de costo/abuso).
- Limpieza de markdown fences en el parseo.
- **Paso B (autorizado):** H1 sanitizado/truncado + system prompt; H2 `max_tokens`; H3 rechazo de array vacío; H4 truncado por campo.
- Tests unitarios: timeout → FALLBACK, provider exception → FALLBACK (`RecommendationServiceTest`).
- Revisión crítica documentada; H7 cerrado en Ej. 3; H5–H6 opcionales.

---

## Ejercicio 3 — Tests e integración

### Prompt(s) usado(s)

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

### Hallazgos / revisión crítica / riesgos detectados

**Casos que generó / se implementaron con apoyo de IA:**

| Caso | Dónde | Estado |
|------|--------|--------|
| 200 AI OK | `RecommendationServiceTest` + `RecommendationEndpointTest` | OK |
| Timeout → FALLBACK | ambos | OK |
| Provider exception → FALLBACK | ambos | OK |
| 401 sin JWT | `RecommendationEndpointTest` (MockMvc) | OK |
| 404 lista inexistente | `RecommendationEndpointTest` (MockMvc) | OK |

**Casos que tuve que añadir / ajustar yo (criterio humano):**

- Extraer `AiResponseParser` y tests de parseo: markdown fence, array vacío, content en blanco, JSON basura (`AiResponseParserTest`) — H7.
- Renombrar `*IT` → `RecommendationEndpointTest` porque Surefire excluye `*IT.java` por defecto (el test no corría).
- Perfil `application-test.yml` + `@MockBean AiRecommendationClient` (sin llamar al proveedor real).
- Test de `sanitizeField` (anti-injection / truncado).

**Aún no cubierto (no bloquea el módulo IA):** test explícito de TTL de caché.

### Decisiones tomadas

- Unit tests del service (baratos) **más** MockMvc con JWT real emitido por `JwtService` (cubre 401/404 de punta a punta).
- Mock del cliente IA en integración; nunca llamar al proveedor real en CI/local de tests.
- Parser aislado en componente propio para poder testear basura del LLM sin HTTP.

### Cumplimiento entregables del módulo (checklist)

| # | Entregable | Estado |
|---|------------|--------|
| 1 | `GET /lists/{listName}/recommendations` + JWT | OK |
| 2 | Cliente IA: credenciales env, errores, timeout, fallback | OK |
| 3 | Tests pasando (incl. fallo del proveedor) | OK (14 tests) |
| 4 | `AI-LOG.md` completo | OK |
| 5 | README Autoría IA + env `AI_API_KEY` | OK |

---

## Front (consola para probar la API)

### Prompt usado

```text
Actúa como Frontend Engineer. Crea una app (Vue 3 + Vite) con:
- Login JWT contra la API
- CRUD listas
- Botón Recomendaciones IA (mostrar source AI/FALLBACK)
Sin AI_API_KEY en el front; token solo en Authorization.
```

### Qué hay

- `frontend/` Vue 3: login, listar/crear/borrar, detalle, recomendaciones.
- Proxy Vite → `:8080`; CORS en API para `localhost:5173`.
- Arranque: `.\scripts\run-front.ps1` (API arriba con `.\scripts\run-api.ps1`).

---

## Reflexión final (obligatoria)

### ¿Dónde falló, alucinó o se equivocó la IA durante esta prueba?

- Suele asumir JSON perfecto del LLM; hubo que añadir limpieza de markdown y manejo de content vacío.
- No contempló prompt injection ni `max_tokens` ni truncado de campos: se aplicaron en Paso B (H1–H4).
- Puede proponer secrets en `application.yml` o ejemplos Postman con tokens reales: se rechazó.
- Puede nombrar tests `*IT` sin advertir que Surefire no los ejecuta: hubo que corregirlo a mano.

### ¿Qué parte del trabajo NO delegarías a la IA y por qué?

- Decisiones de seguridad (dónde viven las keys, qué se loguea, política de fallback).
- Criterio de aceptación Senior y la revisión crítica del código generado (Ejercicio 2).
- Validación manual de que el endpoint no tumba la app ante fallo del proveedor.
- Qué casos de test son obligatorios vs. “nice to have” para la entrega.

### Autoría

| Parte | IA | Mano / criterio humano |
|-------|----|------------------------|
| Arquitectura / diseño | ~70% | ~30% (trade-offs fallback/caché) |
| Equipo + prompts (docs) | ~90% | ~10% (ajuste a enunciado Quipux) |
| API `/lists` + JWT + Spotify | ~80% | ~20% |
| Cliente IA + recommendations | ~70% | ~30% (resiliencia/seguridad) |
| Tests | ~65% | ~35% (MockMvc JWT, parser, fix Surefire `*IT`) |
| Front Vue | ~75% | ~25% (contrato API, sin keys en browser) |
| Documentación (AI-LOG/README) | ~50% | ~50% (honestidad autoría + hallazgos reales) |

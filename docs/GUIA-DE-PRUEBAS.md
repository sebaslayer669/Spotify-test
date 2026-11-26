# Guía de pruebas — qué validar para entregar (Quipux Senior)

Usa esta checklist en orden. Marca cada ítem al verificarlo.

## 0) Preparar workspace (ya instalado)

En PowerShell:

```powershell
cd "C:\Users\sherr\OneDrive\Documentos\prueba Quipux\prueba"
.\scripts\test.ps1          # unit tests
.\scripts\run-api.ps1       # API en http://localhost:8080
```

En **otra** terminal, con la API arriba:

```powershell
cd "C:\Users\sherr\OneDrive\Documentos\prueba Quipux\prueba"
.\scripts\smoke-test.ps1
```

Importa también `postman/api-lists.postman_collection.json` en Postman.

| Pieza | Estado local |
|-------|----------------|
| JDK 17 (Microsoft) | `C:\Program Files\Microsoft\jdk-17.0.21.102-hotspot` |
| Maven | `prueba\.tools\apache-maven-3.10.2` |
| IA por defecto | `AI_ENABLED=false` → cliente mock / fallback (sin API key) |
| LLM real | `$env:AI_ENABLED="true"; $env:AI_API_KEY="..."; .\scripts\run-api.ps1` |

---

## 1) Prueba práctica — API `/lists` (contrato)

| # | Caso | Cómo | Esperado |
|---|------|------|----------|
| 1.1 | Login JWT | `POST /auth/login` `{"username":"demo"}` | 200 + `accessToken` |
| 1.2 | Sin token | `GET /lists` sin Authorization | **401** |
| 1.3 | Crear lista | `POST /lists` + JWT + body válido | **201** + header `Location` + body |
| 1.4 | Nombre inválido | `POST /lists` con `"nombre": null` o `""` | **400** |
| 1.5 | Listar | `GET /lists` | 200 + arreglo |
| 1.6 | Obtener una | `GET /lists/{listName}` | 200 + JSON (nombre, descripcion, canciones) |
| 1.7 | No existe | `GET /lists/no-existe` | **404** |
| 1.8 | Borrar | `DELETE /lists/{listName}` | **204** |
| 1.9 | Borrar inexistente | `DELETE /lists/no-existe` | **404** |
| 1.10 | Persistencia H2 | Crear → reiniciar app → (memoria se pierde; OK en H2 in-memory) o verificar CRUD en misma sesión | CRUD coherente en sesión |

JSON esperado de lista:

```json
{
  "nombre": "Lista 1",
  "descripcion": "...",
  "canciones": [
    { "titulo": "", "artista": "", "album": "", "anno": "", "genero": "" }
  ]
}
```

---

## 2) Spotify (insumo backend)

| # | Caso | Cómo | Esperado |
|---|------|------|----------|
| 2.1 | Sin credenciales | `GET /spotify/genres` + JWT, sin `SPOTIFY_*` | 200 con `genres: []` (no tumba la app) |
| 2.2 | Con credenciales | Set `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` y reinicia | Lista de categorías/géneros |

---

## 3) Módulo IA Senior — `GET /lists/{listName}/recommendations`

| # | Caso | Cómo | Esperado |
|---|------|------|----------|
| 3.1 | Protegido JWT | Sin Bearer | **401** |
| 3.2 | Lista no existe | `/lists/x/recommendations` | **404** |
| 3.3 | Happy path (mock) | Con lista existente, `AI_ENABLED=false` | 200, `source` AI/mock o recomendaciones presentes |
| 3.4 | Fallback sin proveedor | `AI_ENABLED=true` **sin** `AI_API_KEY` | 200 con `source=FALLBACK` (app **no** cae) |
| 3.5 | Timeout / fallo IA | Cubierto por unit test `RecommendationServiceTest` | Fallback, no excepción no controlada |
| 3.6 | Secrets | Revisar repo: no hay keys reales en código | OK (solo `${AI_API_KEY}` / `.env.example`) |
| 3.7 | Prompt versionado | Abrir `api-lists/src/main/resources/prompts/recommendations-v1.txt` | Existe y se usa |
| 3.8 | Costo/abuso | Revisar caché TTL + `AI_MAX_PROMPT_SONGS` en código/config | Documentado / implementado |

Unit tests:

```powershell
.\scripts\test.ps1
```

Debe pasar: AI OK, timeout → FALLBACK, provider exception → FALLBACK.

---

## 4) Frontend (Vue 3 — `frontend/`)

```powershell
.\scripts\run-api.ps1    # terminal 1
.\scripts\run-front.ps1  # terminal 2 → http://localhost:5173
```

| # | Caso | Esperado |
|---|------|----------|
| 4.1 | Login | Guarda JWT y llama API |
| 4.2 | CRUD listas | Formularios crear / listar / ver / borrar |
| 4.3 | Botón recomendaciones | Muestra IA o mensaje de fallback; **sin** `AI_API_KEY` en el front |

> Hoy el front aún está pendiente en el scaffold. Si entregas solo API+IA+docs, indícalo en README/Autoría IA.

---

## 5) Entregables documentales (calificación Senior)

| # | Entregable | Verificar |
|---|------------|-----------|
| 5.1 | `AI-LOG.md` | Prompts pegados, revisión Ej. 2, tests Ej. 3, reflexión |
| 5.2 | `README.md` | Autoría IA + cómo configurar env **sin** exponer valores |
| 5.3 | `docs/PROMPTS-Y-EQUIPO.md` | Catálogo de prompts del equipo |
| 5.4 | Postman | Colección importable y usada en demo |
| 5.5 | Git | Repo con commits visibles (GitHub/GitLab) |
| 5.6 | Security spot-check | Ninguna key en git; app no cae si IA falla |

---

## 6) Orden sugerido de demo (10–15 min)

1. Mostrar `AI-LOG.md` y decisión de arquitectura (interfaz + fallback).
2. Arrancar API → Postman/smoke: login → create → recommendations (`source`).
3. Mostrar unit test de fallo IA.
4. Mostrar prompt versionado + env vars en README.
5. (Si hay) front CRUD + botón IA.
6. Cerrar con reflexión: qué rechazaste de la IA.

## Señales de alerta (evitar en la entrega)

- API keys en el repositorio.
- Endpoint que tumba la app si la IA falla.
- Prompt solo en el chat, no versionado.
- Tests que llaman al proveedor real sin mock.

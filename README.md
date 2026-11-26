# Prueba Técnica Senior — Quipux (API Lists + Módulo IA)

Proyecto ubicado en:

`C:\Users\sherr\OneDrive\Documentos\prueba Quipux\prueba`

Incluye la prueba práctica (API REST `/lists`, JWT, H2, Spotify, front) y el **Módulo de IA Senior** (`GET /lists/{listName}/recommendations`), orquestado con un **equipo de agentes de IA**.

## Estructura

```
prueba/
├── AI-LOG.md                 # Bitácora obligatoria Quipux
├── README.md                 # Este archivo
├── docs/
│   └── PROMPTS-Y-EQUIPO.md   # Equipo + prompts
├── equipo-ia/roles/          # Cartas de rol por agente
├── .cursor/rules/            # Reglas Cursor del equipo
├── api-lists/                # API Maven / Spring Boot
├── frontend/                 # Vue 3 + Vite (consola CRUD + IA)
├── postman/                  # Colección REST
└── scripts/                  # test / run-api / run-front / smoke
```

## Cómo usar el equipo de agentes

1. Abre esta carpeta como workspace en Cursor.
2. Lee `docs/PROMPTS-Y-EQUIPO.md` (roles, prompts, env vars, criterios Senior).
3. Ejecuta por rol: Tech Lead → Backend → QA → Security → Documentador (Frontend en paralelo).
4. Copia cada prompt **realmente usado** en `AI-LOG.md`.

## Variables de entorno (sin valores secretos)

Configura la API key del proveedor de IA **solo en el entorno del servidor** (nunca en el front ni en git):

```powershell
# JWT
$env:JWT_SECRET="change-me-in-local-only"

# Spotify (opcional)
$env:SPOTIFY_CLIENT_ID="your-client-id"
$env:SPOTIFY_CLIENT_SECRET="your-client-secret"

# IA (OpenAI / Gemini compatible / Ollama)
$env:AI_ENABLED="true"                 # false = cliente mock local
$env:AI_API_KEY="your-ai-key"          # NO subir este valor al repo
$env:AI_BASE_URL="https://api.openai.com/v1"
$env:AI_MODEL="gpt-4o-mini"
$env:AI_TIMEOUT_MS="3000"
$env:AI_MAX_PROMPT_SONGS="20"
$env:AI_MAX_TOKENS="500"
$env:AI_MAX_FIELD_CHARS="100"
$env:AI_CACHE_TTL_SECONDS="60"
```

En `application.yml` se leen como placeholders, por ejemplo:

```yaml
app.ai.api-key: ${AI_API_KEY:}
```

Nunca subas valores reales al repositorio ni los pongas en el frontend.

## Arranque local (workspace)

Ya quedó instalado **JDK 17** y **Maven** (portable en `.tools/`). Node.js para el front.

```powershell
cd "C:\Users\sherr\OneDrive\Documentos\prueba Quipux\prueba"
.\scripts\test.ps1          # tests API (incl. fallo del proveedor IA)
.\scripts\run-api.ps1       # http://localhost:8080
# otra terminal:
.\scripts\run-front.ps1     # http://localhost:5173
# opcional:
.\scripts\smoke-test.ps1
```

Checklist de qué probar: [`docs/GUIA-DE-PRUEBAS.md`](docs/GUIA-DE-PRUEBAS.md).

Por defecto conviene `AI_ENABLED=false` (sin API key). Para LLM real:

```powershell
$env:AI_ENABLED="true"
$env:AI_API_KEY="tu-key"
.\scripts\run-api.ps1
```

### Front (Vue 3)

Justificación: Vue 3 + Vite es ligero para una consola de prueba y no requiere tooling Angular pesado.

Flujo: login JWT → crear/listar/ver/borrar listas → **Recomendaciones IA** (muestra `source=AI` o `FALLBACK`).

El front **no** conoce `AI_API_KEY`; solo envía `Authorization: Bearer <jwt>`. En local, Vite hace proxy a `:8080`.

### Deploy front en Vercel

Vercel solo hospeda el **frontend**. La API Spring debe estar en otro host (Render, Railway, Fly.io, VPS, etc.).

En el proyecto Vercel (Root Directory: `frontend`):

| Variable | Valor | Notas |
|----------|-------|--------|
| `VITE_API_BASE_URL` | `https://tu-api.ejemplo.com` | **Obligatoria** en Vercel. Sin slash final. Sin esta var el login falla. |

En el **servidor de la API**:

| Variable | Valor | Notas |
|----------|-------|--------|
| `CORS_ALLOWED_ORIGINS` | `https://tu-app.vercel.app` | Puedes sumar local: `http://localhost:5173,https://tu-app.vercel.app` |
| `AI_ENABLED` | `false` o `true` | `false` = mock local; `true` requiere `AI_API_KEY` |
| `JWT_SECRET` | secreto fuerte | No uses el default de demo en producción |

Build Vercel: Framework Vite, Output `dist`, Install `npm install`, Build `npm run build`.

### Contrato base `/lists`

| Método | URI | Notas |
|--------|-----|--------|
| POST | `/lists` | 201 + Location; 400 nombre inválido |
| GET | `/lists` | Todas las listas |
| GET | `/lists/{listName}` | 404 si no existe |
| DELETE | `/lists/{listName}` | 204 / 404 |
| GET | `/lists/{listName}/recommendations` | JWT; IA + fallback |
| POST | `/auth/login` | Emite JWT demo |

### Probar recommendations (curl / PowerShell)

```powershell
$login = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/auth/login" `
  -ContentType "application/json" -Body '{"username":"demo"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod -Uri "http://localhost:8080/lists/Rock%2090s/recommendations" -Headers $headers
```

Caso fallback: `AI_ENABLED=true` sin `AI_API_KEY` → respuesta 200 con `source=FALLBACK` (la app no cae).

## Autoría IA

| Parte | IA (aprox.) | Mano / criterio humano |
|-------|-------------|------------------------|
| Equipo + prompts (`docs/PROMPTS-Y-EQUIPO.md`) | ~90% | ~10% (ajuste enunciado Quipux) |
| API `/lists` + JWT + Spotify | ~80% | ~20% |
| Cliente IA + recommendations + resiliencia | ~70% | ~30% (fallback, sanitizado, `max_tokens`) |
| Tests (service + MockMvc + parser) | ~65% | ~35% (401/404, Surefire `*IT`, parser) |
| Front Vue (consola) | ~75% | ~25% (contrato, sin keys en browser) |
| `AI-LOG.md` / README | ~50% | ~50% (honestidad, hallazgos reales) |

Detalle de prompts, revisión crítica (Ej. 2) y qué tests añadió la IA vs. a mano: ver [`AI-LOG.md`](AI-LOG.md).

## Entregables Quipux (checklist)

- [x] Endpoint recommendations con JWT
- [x] Cliente IA: secrets, timeout, errores, fallback
- [x] Prompt versionado en repo
- [x] Tests pasando (incl. fallo del proveedor)
- [x] `AI-LOG.md` completo
- [x] README con Autoría IA + instrucciones de env
- [ ] Repo Git con commits evidentes
- [x] Cliente Postman / REST configurado
- [x] Front con formularios CRUD + recomendaciones

## Documentos de origen

En la carpeta padre `prueba Quipux/`: `SENIOR`, `Modulo-IA-Senior`, `PLANTILLA-AI-LOG`, `Teorica-Senior`.

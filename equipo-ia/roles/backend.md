# Rol: Backend Engineer

**Misión:** API `/lists`, JWT, H2, Spotify, cliente IA + `GET /lists/{listName}/recommendations`.

**Usa prompts:** §3.2, §3.3, §3.4 de `docs/PROMPTS-Y-EQUIPO.md`

**Reglas:**
- Secrets solo por env.
- Prompt del LLM versionado en `resources/prompts/`.
- Timeout + fallback obligatorios.
- Interfaz `AiRecommendationClient` testeable.

# chatbot — El asistente de PECDMX (Python)

**En vivo:** https://pecdmx-chatbot.onrender.com/salud (se usa desde la pestaña *Asistente* de https://pecdmx.vercel.app)

Rastrea guías y cotiza consultando al backend, responde dudas con RAG citando la fuente,
clasifica quejas (retraso, daño, extravío u otro) y pasa casos a un empleado.

- Python 3.12 (uv) + FastAPI + LangGraph.
- LLM: Groq (`openai/gpt-oss-120b`) principal y Gemini (`gemini-3.5-flash`) de respaldo.
- Embeddings: `intfloat/multilingual-e5-small` en ONNX int8 (sin PyTorch), con PostgreSQL + pgvector.
- Tests: pytest + Testcontainers, y una evaluación con el LLM real.

## Cómo está organizado
| Archivo | Qué hace |
|---|---|
| `app.py` | Rutas: `/salud`, `/api/chat` y `/api/casos` (empleados) |
| `agente.py` | El grafo de LangGraph (`pensar ⇄ herramientas`), las instrucciones y la verificación de citas |
| `herramientas.py` | Rastrear, cotizar, buscar en políticas, levantar queja y pasar a empleado |
| `llm.py` | Groq con respaldo de Gemini detrás de una interfaz común |
| `embeddings.py` | El modelo e5 en ONNX: texto → vector de 384 números |
| `politicas.py` + `conocimiento.py` | El RAG: parte las políticas por sección y las busca en pgvector |
| `clasificador.py` | Clasificador de quejas (regresión logística sobre e5, pesos de 6 KB) |
| `casos.py` | Quejas y pasos a empleado con folio, en PostgreSQL |
| `gafetes.py` | Valida el JWT que emite el backend |
| `defensas.py` | Defensas contra prompt injection y exceso de mensajes |
| `politicas/*.md` | Las 6 políticas de la empresa que consulta el RAG |
| `datos/quejas*.csv` | Ejemplos para entrenar el clasificador y su examen aparte |

## Comandos
```bash
uv sync                                          # instala las librerías exactas de uv.lock
uv run python scripts/descargar_modelo.py        # baja el modelo e5 (versión fija, verifica SHA-256)
uv run pytest                                    # tests (necesita Docker para pgvector)
uv run ruff check && uv run ruff format --check  # linter y formato
uv run python scripts/evaluar.py                 # evaluación con el LLM real (necesita llaves)
uv run python scripts/entrenar_clasificador.py   # reentrena el clasificador de quejas
uv run python scripts/repetir_pregunta.py "¿puedo mandar un perfume?" 8   # mide la variabilidad del LLM
uv run uvicorn pecdmx_chatbot.app:app --reload   # servidor local en http://localhost:8000
```

## Variables de entorno
Están en [`../.env.example`](../.env.example): `GROQ_API_KEY`, `GEMINI_API_KEY`, `DATABASE_URL`
(formato `postgresql://`, sin `jdbc:`), `PECDMX_JWT_SECRETO` (la misma del backend), `BACKEND_URL`
y `PECDMX_CORS_ORIGENES`.

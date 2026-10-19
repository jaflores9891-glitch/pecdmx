# chatbot — Asistente de servicio al cliente (Python)

Rastrea guías y cotiza consultando al backend, responde dudas con RAG citando la fuente,
clasifica quejas (retraso, daño, extravío) y pasa casos a un empleado.

- Python 3.12 (uv) + FastAPI + LangGraph.
- LLM: Groq principal, Gemini de respaldo.
- Embeddings y clasificador: intfloat/multilingual-e5-small en ONNX.
- Tests: pytest + evaluaciones del chatbot.

> Se construye en la Etapa 4.

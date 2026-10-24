# Receta de la "caja" del chatbot. Dos etapas: la primera instala las librerías con uv y descarga el
# modelo de embeddings; la segunda solo copia lo necesario para correr (sin uv, sin pytest ni ruff).

# --- Etapa 1: instalar ---
FROM python:3.12-slim AS construccion
COPY --from=ghcr.io/astral-sh/uv:0.12 /uv /bin/uv

# Compilar a bytecode acelera el arranque; nunca descargar otro Python: se usa el de la imagen.
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never
WORKDIR /app

# El modelo de embeddings (135 MB) va primero: es lo que menos cambia, así Docker reutiliza esta capa.
# El script descarga una versión fija y verifica las huellas SHA-256; si no coinciden, la construcción falla.
COPY scripts/descargar_modelo.py scripts/
RUN python scripts/descargar_modelo.py /app/modelos

# Después las dependencias: si no cambian, Docker reutiliza esta capa.
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-dev --no-install-project

# Al final el código. --no-editable lo instala dentro de .venv, así la etapa final no necesita src/.
COPY src/ src/
RUN uv sync --locked --no-dev --no-editable

# --- Etapa 2: correr ---
FROM python:3.12-slim
WORKDIR /app

# Nunca correr como administrador (root).
RUN useradd --system --no-create-home pecdmx
COPY --from=construccion /app/modelos /app/modelos
COPY --from=construccion /app/.venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1 MODELO_CARPETA=/app/modelos
USER pecdmx

EXPOSE 8000
# Render indica el puerto en la variable PORT; en tu laptop se usa el 8000.
CMD ["sh", "-c", "uvicorn pecdmx_chatbot.app:app --host 0.0.0.0 --port ${PORT:-8000}"]

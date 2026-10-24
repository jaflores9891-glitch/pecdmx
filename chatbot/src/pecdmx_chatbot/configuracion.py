"""Configuración del chatbot, leída de variables de entorno o del archivo .env (nunca del código)."""

from functools import lru_cache
from pathlib import Path

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Configuracion(BaseSettings):
    """Cada campo se llena con la variable de entorno del mismo nombre en mayúsculas (ej. GROQ_API_KEY)."""

    # Busca el .env en la carpeta actual y en la raíz del repo (cuando se corre desde chatbot/).
    # En Render no hay .env: las variables vienen del panel del servicio.
    # env_ignore_empty: una variable vacía (ej. "GROQ_MODELO=") cuenta como no puesta
    # y se usa el valor por defecto.
    model_config = SettingsConfigDict(
        env_file=(".env", "../.env"), env_file_encoding="utf-8", env_ignore_empty=True, extra="ignore"
    )

    # SecretStr se imprime como "**********": las llaves no se cuelan en los registros.
    groq_api_key: SecretStr | None = None
    groq_modelo: str = "openai/gpt-oss-120b"

    gemini_api_key: SecretStr | None = None
    # Versión fija (no "-latest", que Google cambia sin avisar).
    # gemini-3.8-flash daba errores 504 (sin respuesta a tiempo) en el plan gratis.
    gemini_modelo: str = "gemini-3.5-flash"

    # Qué tan "creativo" es el modelo: bajo, porque un asistente de servicio debe ser preciso.
    llm_temperatura: float = 0.2
    # Cuánto esperar a un proveedor antes de darlo por caído y pasar al respaldo.
    llm_espera_segundos: float = 30.0

    # Dónde está el modelo de embeddings (lo baja scripts/descargar_modelo.py).
    # Por defecto, chatbot/modelos/; en Docker se indica con la variable MODELO_CARPETA.
    modelo_carpeta: Path = Path(__file__).resolve().parents[2] / "modelos"

    # Las políticas de PECDMX que consulta el RAG (archivos .md). En Docker: POLITICAS_CARPETA.
    politicas_carpeta: Path = Path(__file__).resolve().parents[2] / "politicas"

    # PostgreSQL con pgvector (Neon en producción), formato postgresql://usuario:contraseña@host/base
    database_url: SecretStr | None = None


@lru_cache
def configuracion() -> Configuracion:
    """La configuración se lee una sola vez y se reutiliza."""
    return Configuracion()

"""La aplicación web del chatbot: aquí se registran todas sus ventanillas (rutas)."""

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator

from pecdmx_chatbot.agente import Asistente, Mensaje
from pecdmx_chatbot.configuracion import Configuracion, configuracion

registro = logging.getLogger("pecdmx_chatbot")


def armar_asistente(config: Configuracion) -> Asistente | None:
    """Junta todas las piezas. Si falta configuración, el chatbot arranca sin asistente (responde 503)."""
    if not (config.groq_api_key or config.gemini_api_key) or not config.database_url:
        registro.warning("Falta GROQ_API_KEY/GEMINI_API_KEY o DATABASE_URL: /api/chat no estará disponible.")
        return None

    # Importados aquí: cargar el modelo y la base solo hace falta si el asistente se va a armar.
    from pecdmx_chatbot.conocimiento import BaseDeConocimiento
    from pecdmx_chatbot.embeddings import Incrustador
    from pecdmx_chatbot.herramientas import crear_herramientas
    from pecdmx_chatbot.llm import crear_llm
    from pecdmx_chatbot.negocio import ClienteDeNegocio
    from pecdmx_chatbot.politicas import leer_politicas

    conocimiento = BaseDeConocimiento(
        config.database_url.get_secret_value(), Incrustador(config.modelo_carpeta)
    )
    conocimiento.crear_esquema()
    if conocimiento.indexar(leer_politicas(config.politicas_carpeta)):
        registro.info("Políticas indexadas de nuevo (cambiaron desde la última vez).")

    herramientas = crear_herramientas(ClienteDeNegocio(config.backend_url), conocimiento)
    return Asistente(crear_llm(config, herramientas), herramientas)


@asynccontextmanager
async def ciclo_de_vida(app: FastAPI) -> AsyncIterator[None]:
    """Lo que se hace una vez al arrancar el servidor (antes de atender a nadie)."""
    app.state.asistente = armar_asistente(configuracion())
    yield


app = FastAPI(
    title="PECDMX — Chatbot",
    description="Asistente de servicio al cliente de PECDMX.",
    version="0.1.0",
    lifespan=ciclo_de_vida,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=configuracion().origenes_permitidos,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization"],
)


class MensajeJson(BaseModel):
    rol: Literal["cliente", "asistente"]
    texto: str = Field(min_length=1, max_length=1000)


class SolicitudChatJson(BaseModel):
    """La conversación completa. El chatbot no guarda el historial: la página lo manda cada vez."""

    mensajes: list[MensajeJson] = Field(min_length=1, max_length=20)

    @field_validator("mensajes")
    @classmethod
    def el_ultimo_es_del_cliente(cls, mensajes: list[MensajeJson]) -> list[MensajeJson]:
        if mensajes[-1].rol != "cliente":
            raise ValueError("El último mensaje debe ser del cliente.")
        return mensajes


class RespuestaChatJson(BaseModel):
    respuesta: str
    fuentes: list[str]


def obtener_asistente(request: Request) -> Asistente:
    asistente = getattr(request.app.state, "asistente", None)
    if asistente is None:
        raise HTTPException(503, "El asistente no está disponible en este momento.")
    return asistente


@app.get("/salud")
def salud() -> dict[str, str]:
    """Para saber si el servicio está vivo (Render la usa para revisar que arrancó)."""
    return {"estado": "ok"}


# "def" normal (no async): FastAPI la corre en un hilo aparte, así una respuesta lenta no frena a las demás.
@app.post("/api/chat")
def chatear(
    solicitud: SolicitudChatJson, asistente: Annotated[Asistente, Depends(obtener_asistente)]
) -> RespuestaChatJson:
    conversacion = [Mensaje(m.rol, m.texto) for m in solicitud.mensajes]
    try:
        respuesta = asistente.responder(conversacion)
    except Exception as error:
        # Los dos LLM fallaron (o algo inesperado): se registra y al cliente se le da un mensaje claro.
        registro.exception("No se pudo responder el chat", exc_info=error)
        raise HTTPException(
            503, "El asistente no pudo responder. Intenta de nuevo en unos minutos."
        ) from error
    return RespuestaChatJson(respuesta=respuesta.texto, fuentes=respuesta.fuentes)

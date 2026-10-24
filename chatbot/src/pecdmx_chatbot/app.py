"""La aplicación web del chatbot: aquí se registran todas sus ventanillas (rutas)."""

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field, field_validator

from pecdmx_chatbot.agente import Asistente, Mensaje
from pecdmx_chatbot.casos import Caso, CasoNoEncontradoError, RepositorioDeCasos
from pecdmx_chatbot.configuracion import Configuracion, configuracion
from pecdmx_chatbot.defensas import DetectorDeInyeccion, LimiteDeMensajes
from pecdmx_chatbot.gafetes import GafeteInvalidoError, Operador, leer_gafete

registro = logging.getLogger("pecdmx_chatbot")


def armar_asistente(config: Configuracion, casos: RepositorioDeCasos) -> Asistente | None:
    """Junta todas las piezas. Si falta configuración, el chatbot arranca sin asistente (responde 503)."""
    if not (config.groq_api_key or config.gemini_api_key) or not config.database_url:
        registro.warning("Falta GROQ_API_KEY/GEMINI_API_KEY o DATABASE_URL: /api/chat no estará disponible.")
        return None

    # Importados aquí: cargar el modelo y la base solo hace falta si el asistente se va a armar.
    from pecdmx_chatbot.clasificador import ClasificadorDeQuejas
    from pecdmx_chatbot.conocimiento import BaseDeConocimiento
    from pecdmx_chatbot.embeddings import Incrustador
    from pecdmx_chatbot.herramientas import crear_herramientas
    from pecdmx_chatbot.llm import crear_llm
    from pecdmx_chatbot.negocio import ClienteDeNegocio
    from pecdmx_chatbot.politicas import leer_politicas

    incrustador = Incrustador(config.modelo_carpeta)
    conocimiento = BaseDeConocimiento(config.database_url.get_secret_value(), incrustador)
    conocimiento.crear_esquema()
    if conocimiento.indexar(leer_politicas(config.politicas_carpeta)):
        registro.info("Políticas indexadas de nuevo (cambiaron desde la última vez).")

    herramientas = crear_herramientas(
        ClienteDeNegocio(config.backend_url), conocimiento, casos, ClasificadorDeQuejas(incrustador)
    )
    detector = (
        DetectorDeInyeccion.con_groq(config.groq_api_key.get_secret_value()) if config.groq_api_key else None
    )
    return Asistente(crear_llm(config, herramientas), herramientas, detector)


def armar_casos(config: Configuracion) -> RepositorioDeCasos | None:
    if not config.database_url:
        return None
    casos = RepositorioDeCasos(config.database_url.get_secret_value())
    casos.crear_esquema()
    return casos


@asynccontextmanager
async def ciclo_de_vida(app: FastAPI) -> AsyncIterator[None]:
    """Lo que se hace una vez al arrancar el servidor (antes de atender a nadie)."""
    config = configuracion()
    app.state.casos = armar_casos(config)
    app.state.asistente = armar_asistente(config, app.state.casos) if app.state.casos else None
    secreto = config.pecdmx_jwt_secreto
    app.state.jwt_secreto = secreto.get_secret_value() if secreto else None
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
        # Tope a la conversación completa: un texto enorme gasta el límite gratis del LLM y esconde ataques.
        if sum(len(m.texto) for m in mensajes) > 6000:
            raise ValueError("La conversación es demasiado larga. Empieza una nueva.")
        return mensajes


class RespuestaChatJson(BaseModel):
    respuesta: str
    fuentes: list[str]


# Por persona (su IP) y para todo el servidor: así nadie agota solo el límite gratis de Groq.
limite_por_persona = LimiteDeMensajes(maximo=10, ventana_segundos=60)
limite_global = LimiteDeMensajes(maximo=60, ventana_segundos=60)


def ip_del_cliente(request: Request) -> str:
    # En Render la petición pasa por un intermediario, que pone la IP real en X-Forwarded-For.
    reenviada = request.headers.get("x-forwarded-for", "")
    return reenviada.split(",")[0].strip() or (request.client.host if request.client else "desconocida")


def revisar_limites(request: Request) -> None:
    if not limite_por_persona.permitir(ip_del_cliente(request)) or not limite_global.permitir("todos"):
        raise HTTPException(
            429, "Vas muy rápido. Espera un minuto y vuelve a intentarlo.", {"Retry-After": "60"}
        )


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
@app.post("/api/chat", dependencies=[Depends(revisar_limites)])
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


# --- Casos (quejas y pasos a empleado): solo empleados y gerentes, con el gafete del backend ---

# auto_error=False: si no hay gafete, respondemos nosotros con 401 y un mensaje en español.
portador = HTTPBearer(auto_error=False)


def obtener_casos(request: Request) -> RepositorioDeCasos:
    casos = getattr(request.app.state, "casos", None)
    if casos is None:
        raise HTTPException(503, "Los casos no están disponibles en este momento.")
    return casos


def obtener_operador(
    request: Request, credenciales: Annotated[HTTPAuthorizationCredentials | None, Depends(portador)]
) -> Operador:
    secreto = getattr(request.app.state, "jwt_secreto", None)
    if secreto is None:
        raise HTTPException(503, "El acceso de empleados no está configurado.")
    if credenciales is None:
        raise HTTPException(401, "Inicia sesión para ver los casos.", {"WWW-Authenticate": "Bearer"})
    try:
        return leer_gafete(credenciales.credentials, secreto)
    except GafeteInvalidoError as error:
        raise HTTPException(
            401, "Tu sesión no es válida o ya expiró.", {"WWW-Authenticate": "Bearer"}
        ) from error


class CierreJson(BaseModel):
    respuesta: str = Field(min_length=1, max_length=2000)


@app.get("/api/casos")
def listar_casos(
    casos: Annotated[RepositorioDeCasos, Depends(obtener_casos)],
    operador: Annotated[Operador, Depends(obtener_operador)],
    estado: Literal["ABIERTO", "CERRADO"] = "ABIERTO",
) -> list[Caso]:
    """Un empleado ve los casos de su sucursal (y los que no tienen sucursal); un gerente, todos."""
    return casos.listar(estado, None if operador.rol == "GERENTE" else operador.sucursal)


@app.post("/api/casos/{folio}/cierre")
def cerrar_caso(
    folio: str,
    cierre: CierreJson,
    casos: Annotated[RepositorioDeCasos, Depends(obtener_casos)],
    operador: Annotated[Operador, Depends(obtener_operador)],
) -> Caso:
    try:
        caso = casos.buscar(folio)
        if not operador.puede_ver(caso.sucursal):
            raise HTTPException(403, "Ese caso es de otra sucursal.")
        return casos.cerrar(folio, operador.correo, cierre.respuesta)
    except CasoNoEncontradoError as error:
        raise HTTPException(404, f"No hay un caso abierto con el folio {folio}.") from error

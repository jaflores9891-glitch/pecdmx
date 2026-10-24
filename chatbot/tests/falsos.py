"""Piezas falsas para probar el agente sin internet: un LLM con guion, un backend y un RAG de mentira."""

import json
from collections.abc import Callable

import httpx
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage
from langchain_core.outputs import ChatGeneration, ChatResult

from pecdmx_chatbot.casos import CasoNuevo
from pecdmx_chatbot.clasificador import Clasificacion
from pecdmx_chatbot.conocimiento import Resultado
from pecdmx_chatbot.herramientas import crear_herramientas
from pecdmx_chatbot.negocio import ClienteDeNegocio
from pecdmx_chatbot.politicas import Fragmento


class LLMConGuion(BaseChatModel):
    """Responde, en orden, los mensajes de su guion; y anota todo lo que recibió en cada llamada."""

    guion: list[AIMessage]
    recibido: list[list[BaseMessage]] = []

    @property
    def _llm_type(self) -> str:
        return "con-guion"

    def _generate(self, messages, stop=None, run_manager=None, **kwargs) -> ChatResult:
        self.recibido.append(list(messages))
        siguiente = self.guion.pop(0) if len(self.guion) > 1 else self.guion[0]
        # Una copia nueva cada vez: LangGraph reemplaza (no agrega) un mensaje cuyo id ya vio.
        return ChatResult(generations=[ChatGeneration(message=siguiente.model_copy(deep=True))])

    def bind_tools(self, tools, **kwargs):
        return self


def pide(herramienta: str, **argumentos) -> AIMessage:
    """Un mensaje del LLM que pide usar una herramienta."""
    return AIMessage(content="", tool_calls=[{"name": herramienta, "args": argumentos, "id": "llamada-1"}])


RASTREO = {
    "numero": "PEC0000000018",
    "origen": {"codigo": "CDMX", "ciudad": "Ciudad de México", "estado": "Ciudad de México"},
    "destino": {"codigo": "MTY", "ciudad": "Monterrey", "estado": "Nuevo León"},
    "estado": {"codigo": "EN_TRANSITO", "nombre": "En tránsito", "descripcion": "Va en camino."},
    "historial": [
        {
            "estado": {"codigo": "REGISTRADA", "nombre": "Registrada", "descripcion": "..."},
            "fechaHora": "2026-10-20T15:00:00Z",
            "sucursal": {"codigo": "CDMX", "ciudad": "Ciudad de México", "estado": "Ciudad de México"},
            "nota": None,
        },
        {
            "estado": {"codigo": "EN_TRANSITO", "nombre": "En tránsito", "descripcion": "..."},
            "fechaHora": "2026-10-21T02:30:00Z",
            "sucursal": {"codigo": "CDMX", "ciudad": "Ciudad de México", "estado": "Ciudad de México"},
            "nota": "Salió en el camión de la noche",
        },
    ],
}

COTIZACION = {
    "origen": {"codigo": "CDMX", "ciudad": "Ciudad de México", "estado": "Ciudad de México"},
    "destino": {"codigo": "GDL", "ciudad": "Guadalajara", "estado": "Jalisco"},
    "zona": 2,
    "descripcionZona": "Regional",
    "pesoRealKg": 3.5,
    "pesoVolumetricoKg": 4.8,
    "pesoCobrableKg": 5,
    "subtotal": 310.00,
    "iva": 49.60,
    "total": 359.60,
    "moneda": "MXN",
}


def backend_falso(responder: Callable[[httpx.Request], httpx.Response]) -> ClienteDeNegocio:
    return ClienteDeNegocio("http://backend", transporte=httpx.MockTransport(responder))


def backend_que_sabe_todo() -> ClienteDeNegocio:
    def responder(peticion: httpx.Request) -> httpx.Response:
        if peticion.url.path == "/api/cotizaciones":
            return httpx.Response(200, json=COTIZACION)
        if peticion.url.path == "/api/guias/PEC0000000018":
            return httpx.Response(200, json=RASTREO)
        return httpx.Response(404, json={"detail": "No existe la guía."})

    return backend_falso(responder)


def problema(estado: int, **campos) -> httpx.Response:
    return httpx.Response(
        estado, content=json.dumps(campos), headers={"Content-Type": "application/problem+json"}
    )


class CasosFalsos:
    """Guarda los casos en una lista, en memoria."""

    def __init__(self) -> None:
        self.registrados: list[CasoNuevo] = []

    def registrar(self, caso: CasoNuevo) -> str:
        self.registrados.append(caso)
        return f"{'Q' if caso.tipo == 'QUEJA' else 'A'}-{len(self.registrados):06d}"


class ClasificadorFalso:
    def clasificar(self, texto: str) -> Clasificacion:
        return Clasificacion("DANO" if "roto" in texto else "OTRO", 0.9)


def herramientas_falsas(backend: ClienteDeNegocio | None = None, casos: CasosFalsos | None = None) -> list:
    return crear_herramientas(
        backend or backend_que_sabe_todo(), ConocimientoFalso(), casos or CasosFalsos(), ClasificadorFalso()
    )


class ConocimientoFalso:
    """Siempre encuentra las mismas 2 secciones."""

    def buscar(self, pregunta: str, cuantos: int = 3) -> list[Resultado]:
        return [
            Resultado(Fragmento("a.md", "Artículos prohibidos", "Pilas", "No se envían pilas sueltas."), 0.9),
            Resultado(Fragmento("b.md", "Embalaje", "Frágiles", "Usa burbuja."), 0.7),
        ]

"""Las herramientas que el LLM puede usar: rastrear, cotizar y buscar en las políticas.

El LLM no ejecuta nada por sí mismo: pide "usa rastrear_guia con PEC..." y el agente la corre.
La descripción (docstring) de cada herramienta es lo que el LLM lee para decidir cuándo usarla.
Las herramientas nunca lanzan errores al LLM: regresan un texto que explica qué pasó, para que lo cuente.
"""

from datetime import datetime
from typing import Any, Literal
from zoneinfo import ZoneInfo

from langchain_core.tools import BaseTool, tool

from pecdmx_chatbot.conocimiento import BaseDeConocimiento
from pecdmx_chatbot.negocio import BackendNoDisponibleError, ClienteDeNegocio, ErrorDelNegocio

HORA_DE_MEXICO = ZoneInfo("America/Mexico_City")
CodigoDeSucursal = Literal["CDMX", "GDL", "MTY", "PUE", "QRO", "TIJ"]

SIN_SISTEMA = (
    "El sistema de PECDMX no respondió en este momento. "
    "Pide al cliente que lo intente de nuevo en un par de minutos."
)


def crear_herramientas(negocio: ClienteDeNegocio, conocimiento: BaseDeConocimiento) -> list[BaseTool]:
    @tool
    def rastrear_guia(numero: str) -> str:
        """Consulta el estado actual y el historial de un envío de PECDMX.

        Úsala siempre que el cliente pregunte por su paquete y dé un número de guía (PEC y 10 dígitos).
        Si no tiene el número, pídeselo; nunca lo inventes.
        """
        try:
            return _texto_de_rastreo(negocio.rastrear(numero))
        except ErrorDelNegocio as error:
            return f"No se pudo rastrear: {error}"
        except BackendNoDisponibleError:
            return SIN_SISTEMA

    @tool
    def cotizar_envio(
        origen: CodigoDeSucursal,
        destino: CodigoDeSucursal,
        peso_kg: float,
        largo_cm: int,
        ancho_cm: int,
        alto_cm: int,
    ) -> str:
        """Calcula el precio exacto de un envío entre dos sucursales de PECDMX, con IVA.

        Sucursales: CDMX (Ciudad de México), GDL (Guadalajara), MTY (Monterrey), PUE (Puebla),
        QRO (Querétaro) y TIJ (Tijuana). Necesita el peso en kg y las medidas de la caja en cm;
        si el cliente no las dio, pídeselas antes de usarla. Nunca calcules precios por tu cuenta.
        """
        try:
            cotizacion = negocio.cotizar(origen, destino, peso_kg, largo_cm, ancho_cm, alto_cm)
            return _texto_de_cotizacion(cotizacion)
        except ErrorDelNegocio as error:
            return f"No se pudo cotizar: {error}"
        except BackendNoDisponibleError:
            return SIN_SISTEMA

    # content_and_artifact: el texto va al LLM y las citas encontradas se guardan aparte (el "artefacto"),
    # para después comprobar que el LLM solo cite fuentes que de verdad se consultaron.
    @tool(response_format="content_and_artifact")
    def buscar_politicas(pregunta: str) -> tuple[str, list[str]]:
        """Busca en las políticas oficiales de PECDMX.

        Úsala para CUALQUIER duda sobre: artículos prohibidos, ciudades y horarios, cómo se cobra
        (peso, peso volumétrico, IVA), entrega y recolección, cancelaciones, quejas, reembolsos y embalaje.
        Escribe la duda del cliente como una pregunta completa.
        """
        resultados = conocimiento.buscar(pregunta, cuantos=3)
        texto = "\n\n".join(f"[Fuente: {r.fragmento.cita}]\n{r.fragmento.contenido}" for r in resultados)
        return texto or "No se encontró nada en las políticas.", [r.fragmento.cita for r in resultados]

    return [rastrear_guia, cotizar_envio, buscar_politicas]


def _fecha(iso: str) -> str:
    """'2026-10-20T15:00:00Z' (UTC) → '20/10/2026 09:00' en hora del centro de México."""
    fecha = datetime.fromisoformat(iso.replace("Z", "+00:00")).astimezone(HORA_DE_MEXICO)
    return fecha.strftime("%d/%m/%Y %H:%M")


def _sucursal(sucursal: dict[str, Any]) -> str:
    return f"{sucursal['ciudad']} ({sucursal['codigo']})"


def _texto_de_rastreo(rastreo: dict[str, Any]) -> str:
    estado = rastreo["estado"]
    lineas = [
        f"Guía {rastreo['numero']}: de {_sucursal(rastreo['origen'])} a {_sucursal(rastreo['destino'])}.",
        f"Estado actual: {estado['nombre']} — {estado['descripcion']}",
        "Historial (hora del centro de México):",
    ]
    for evento in rastreo["historial"]:
        nota = f" ({evento['nota']})" if evento.get("nota") else ""
        lineas.append(
            f"- {_fecha(evento['fechaHora'])}: {evento['estado']['nombre']} en "
            f"{_sucursal(evento['sucursal'])}{nota}"
        )
    return "\n".join(lineas)


def _texto_de_cotizacion(c: dict[str, Any]) -> str:
    return (
        f"Envío de {_sucursal(c['origen'])} a {_sucursal(c['destino'])} "
        f"(zona {c['zona']}: {c['descripcionZona']}).\n"
        f"Peso real {c['pesoRealKg']} kg, peso volumétrico {c['pesoVolumetricoKg']} kg; "
        f"se cobran {c['pesoCobrableKg']} kg.\n"
        f"Subtotal ${c['subtotal']}, IVA ${c['iva']}, total ${c['total']} {c['moneda']}."
    )

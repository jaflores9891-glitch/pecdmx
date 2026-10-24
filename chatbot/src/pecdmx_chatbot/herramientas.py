"""Las herramientas que el LLM puede usar: rastrear, cotizar, buscar en las políticas, levantar quejas
y pasar el caso a un empleado.

El LLM no ejecuta nada por sí mismo: pide "usa rastrear_guia con PEC..." y el agente la corre.
La descripción (docstring) de cada herramienta es lo que el LLM lee para decidir cuándo usarla.
Las herramientas nunca lanzan errores al LLM: regresan un texto que explica qué pasó, para que lo cuente.
"""

import re
from datetime import datetime
from typing import Any, Literal
from zoneinfo import ZoneInfo

from langchain_core.tools import BaseTool, tool

from pecdmx_chatbot.casos import CasoNuevo, RepositorioDeCasos
from pecdmx_chatbot.clasificador import NOMBRES, ClasificadorDeQuejas
from pecdmx_chatbot.conocimiento import BaseDeConocimiento
from pecdmx_chatbot.negocio import BackendNoDisponibleError, ClienteDeNegocio, ErrorDelNegocio

HORA_DE_MEXICO = ZoneInfo("America/Mexico_City")
CodigoDeSucursal = Literal["CDMX", "GDL", "MTY", "PUE", "QRO", "TIJ"]
CORREO = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")
LARGO_MAXIMO = 1000

SIN_SISTEMA = (
    "El sistema de PECDMX no respondió en este momento. "
    "Pide al cliente que lo intente de nuevo en un par de minutos."
)


def crear_herramientas(
    negocio: ClienteDeNegocio,
    conocimiento: BaseDeConocimiento,
    casos: RepositorioDeCasos,
    clasificador: ClasificadorDeQuejas,
) -> list[BaseTool]:
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
        if not resultados:
            return "No se encontró nada en las políticas.", []
        texto = "\n\n".join(f"[Fuente: {r.fragmento.cita}]\n{r.fragmento.contenido}" for r in resultados)
        # El recordatorio va junto al resultado: el LLM lo ve justo antes de responder (en la evaluación,
        # con la regla solo en las instrucciones, a veces olvidaba citar).
        recordatorio = "\n\nAl responder, termina con la fuente que usaste, copiada tal cual: [Fuente: …]"
        return texto + recordatorio, [r.fragmento.cita for r in resultados]

    @tool
    def levantar_queja(numero_guia: str, descripcion: str, contacto: str) -> str:
        """Registra una queja formal sobre un envío (retraso, daño, extravío u otro problema) y da su folio.

        Antes de usarla, pide al cliente: el número de guía, qué pasó (con sus palabras) y un correo
        o teléfono para responderle. Usa la descripción del cliente tal como la dio.
        """
        if not _es_contacto_valido(contacto):
            return "Falta un contacto válido: pide al cliente un correo o un teléfono de 10 dígitos."
        try:
            sucursal = negocio.rastrear(numero_guia)["destino"]["codigo"]
        except ErrorDelNegocio as error:
            return f"No se pudo levantar la queja: {error}"
        except BackendNoDisponibleError:
            sucursal = None  # se registra igual; la verá cualquier empleado
        clasificacion = clasificador.clasificar(descripcion)
        folio = casos.registrar(
            CasoNuevo(
                tipo="QUEJA",
                descripcion=descripcion[:LARGO_MAXIMO],
                contacto=contacto[:200],
                guia=numero_guia.strip().upper(),
                sucursal=sucursal,
                categoria=clasificacion.categoria,
                confianza=clasificacion.confianza,
            )
        )
        return (
            f"Queja registrada con folio {folio}. Tipo: {NOMBRES[clasificacion.categoria]}. "
            f"Un empleado la revisará y responderá a {contacto} en un máximo de 3 días hábiles. "
            "Dale al cliente su folio."
        )

    @tool
    def pasar_a_empleado(motivo: str, contacto: str, numero_guia: str = "") -> str:
        """Pasa el caso a un empleado de PECDMX, que contactará al cliente.

        Úsala cuando no puedas resolver lo que pide el cliente, o cuando pida hablar con una persona.
        Antes, pide un correo o teléfono para contactarlo. El número de guía es opcional.
        """
        if not _es_contacto_valido(contacto):
            return "Falta un contacto válido: pide al cliente un correo o un teléfono de 10 dígitos."
        folio = casos.registrar(
            CasoNuevo(
                tipo="ATENCION",
                descripcion=motivo[:LARGO_MAXIMO],
                contacto=contacto[:200],
                guia=numero_guia.strip().upper() or None,
            )
        )
        return (
            f"Caso pasado a un empleado con folio {folio}. Contactará al cliente en {contacto} "
            "dentro del horario de atención. Dale al cliente su folio."
        )

    return [rastrear_guia, cotizar_envio, buscar_politicas, levantar_queja, pasar_a_empleado]


def _es_contacto_valido(contacto: str) -> bool:
    """Un correo (algo@algo.algo) o un teléfono con al menos 10 dígitos."""
    return bool(CORREO.fullmatch(contacto.strip())) or len(re.sub(r"\D", "", contacto)) >= 10


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
        f"Subtotal {_pesos(c['subtotal'])}, IVA {_pesos(c['iva'])}, total {_pesos(c['total'])} {c['moneda']}."
    )


def _pesos(monto: float | str) -> str:
    """359.6 → "$359.60": siempre con 2 decimales (el JSON pierde el cero final) y comas de miles."""
    return f"${float(monto):,.2f}"

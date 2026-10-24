"""Evaluación del chatbot con el LLM REAL: el "examen final" de comportamiento.

Los tests (pytest) usan un LLM con guion: prueban el código, pero no si el LLM real decide bien.
Este script le hace al LLM real las preguntas de evaluaciones/casos.json y revisa cada respuesta:
qué herramientas usó, qué fuente citó y qué dice (o no dice).

Lo que no es el LLM se simula, para que el resultado no dependa de que Render esté despierto:
un backend falso (con una guía y una cotización conocidas), las políticas en memoria y los casos en una lista.

Uso (desde chatbot/, con GROQ_API_KEY y/o GEMINI_API_KEY en el .env):
    uv run python scripts/evaluar.py
Termina con error si aprueba menos del MINIMO de los casos (el LLM no siempre responde igual).
"""

import json
import os
import sys
import time
import unicodedata
from pathlib import Path

import httpx
import numpy as np

from pecdmx_chatbot.agente import Asistente, Mensaje, Respuesta
from pecdmx_chatbot.casos import CasoNuevo
from pecdmx_chatbot.clasificador import ClasificadorDeQuejas
from pecdmx_chatbot.configuracion import configuracion
from pecdmx_chatbot.conocimiento import Resultado
from pecdmx_chatbot.defensas import RESPUESTA_A_UN_ATAQUE, DetectorDeInyeccion
from pecdmx_chatbot.embeddings import Incrustador
from pecdmx_chatbot.herramientas import crear_herramientas
from pecdmx_chatbot.llm import crear_llm
from pecdmx_chatbot.negocio import ClienteDeNegocio
from pecdmx_chatbot.politicas import leer_politicas

CASOS = Path(__file__).resolve().parents[1] / "evaluaciones" / "casos.json"
MINIMO = 0.85
PAUSA_SEGUNDOS = 2.0  # entre preguntas, para no rebasar el límite por minuto del plan gratis

SUCURSALES = {
    "CDMX": "Ciudad de México", "GDL": "Guadalajara", "MTY": "Monterrey",
    "PUE": "Puebla", "QRO": "Querétaro", "TIJ": "Tijuana",
}  # fmt: skip


def sucursal(codigo: str) -> dict:
    return {"codigo": codigo, "ciudad": SUCURSALES[codigo], "estado": SUCURSALES[codigo]}


def backend_de_prueba(peticion: httpx.Request) -> httpx.Response:
    """Una guía conocida (PEC0000000018, en tránsito) y la cotización real de 3.5 kg CDMX→GDL ($359.60)."""
    if peticion.url.path == "/api/guias/PEC0000000018":
        en_transito = {"codigo": "EN_TRANSITO", "nombre": "En tránsito", "descripcion": "Va en camino."}
        evento = {
            "estado": en_transito,
            "fechaHora": "2026-10-21T02:30:00Z",
            "sucursal": sucursal("CDMX"),
            "nota": None,
        }
        return httpx.Response(
            200,
            json={
                "numero": "PEC0000000018",
                "origen": sucursal("CDMX"),
                "destino": sucursal("MTY"),
                "estado": en_transito,
                "historial": [evento],
            },
        )
    if peticion.url.path == "/api/cotizaciones":
        datos = json.loads(peticion.content)
        if (datos["origen"], datos["destino"], datos["pesoKg"]) == ("CDMX", "GDL", 3.5):
            return httpx.Response(
                200,
                json={
                    "origen": sucursal("CDMX"),
                    "destino": sucursal("GDL"),
                    "zona": 2,
                    "descripcionZona": "Regional",
                    "pesoRealKg": 3.5,
                    "pesoVolumetricoKg": 4.8,
                    "pesoCobrableKg": 5,
                    "subtotal": "310.00",
                    "iva": "49.60",
                    "total": "359.60",
                    "moneda": "MXN",
                },
            )
        return httpx.Response(422, json={"detail": "En la evaluación solo se cotiza 3.5 kg de CDMX a GDL."})
    return httpx.Response(404, json={"detail": f"No existe la guía {peticion.url.path.rsplit('/', 1)[-1]}."})


class PoliticasEnMemoria:
    """Igual que BaseDeConocimiento, pero sin base de datos: busca con numpy sobre las mismas políticas."""

    def __init__(self, incrustador: Incrustador) -> None:
        self._incrustador = incrustador
        self._fragmentos = leer_politicas(configuracion().politicas_carpeta)
        self._vectores = incrustador.incrustar_documentos([f.texto_para_buscar for f in self._fragmentos])

    def buscar(self, pregunta: str, cuantos: int = 3) -> list[Resultado]:
        similitudes = self._vectores @ self._incrustador.incrustar_consulta(pregunta)
        mejores = np.argsort(-similitudes)[:cuantos]
        return [Resultado(self._fragmentos[i], float(similitudes[i])) for i in mejores]


class CasosEnMemoria:
    def __init__(self) -> None:
        self.registrados: list[CasoNuevo] = []

    def registrar(self, caso: CasoNuevo) -> str:
        self.registrados.append(caso)
        return f"{'Q' if caso.tipo == 'QUEJA' else 'A'}-{len(self.registrados):06d}"


def normalizar(texto: str) -> str:
    """Sin acentos ni mayúsculas, para comparar "Tránsito" con "transito"."""
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn").lower()


def revisar(caso: dict, respuesta: Respuesta, casos_nuevos: int) -> list[str]:
    """Regresa la lista de problemas; vacía si la respuesta aprueba."""
    texto = normalizar(respuesta.texto)
    problemas = []
    if caso.get("sin_casos_nuevos") and casos_nuevos:
        problemas.append("guardó un caso sin deber")
    if caso.get("bloqueado") and respuesta.texto != RESPUESTA_A_UN_ATAQUE:
        problemas.append("no se bloqueó el ataque")
    for herramienta in caso.get("herramientas", []):
        if herramienta not in respuesta.herramientas_usadas:
            problemas.append(f"no usó {herramienta}")
    for herramienta in caso.get("sin_herramientas", []):
        if herramienta in respuesta.herramientas_usadas:
            problemas.append(f"usó {herramienta} sin deber")
    if caso.get("fuente") and not any(f.endswith(caso["fuente"]) for f in respuesta.fuentes):
        problemas.append(f"no citó «{caso['fuente']}» (citó {respuesta.fuentes})")
    for frase in caso.get("contiene", []):
        if normalizar(frase) not in texto:
            problemas.append(f"no dice «{frase}»")
    if caso.get("alguno") and not any(normalizar(f) in texto for f in caso["alguno"]):
        problemas.append(f"no dice ninguna de {caso['alguno']}")
    for frase in caso.get("no_contiene", []):
        if normalizar(frase) in texto:
            problemas.append(f"dice «{frase}»")
    return problemas


def main() -> int:
    config = configuracion()
    if not (config.groq_api_key or config.gemini_api_key):
        print("Sin GROQ_API_KEY ni GEMINI_API_KEY: no se puede evaluar.")
        return 1

    incrustador = Incrustador(config.modelo_carpeta)
    negocio = ClienteDeNegocio("http://backend-de-prueba", transporte=httpx.MockTransport(backend_de_prueba))
    registro_de_casos = CasosEnMemoria()
    herramientas = crear_herramientas(
        negocio, PoliticasEnMemoria(incrustador), registro_de_casos, ClasificadorDeQuejas(incrustador)
    )
    detector = (
        DetectorDeInyeccion.con_groq(config.groq_api_key.get_secret_value()) if config.groq_api_key else None
    )
    asistente = Asistente(crear_llm(config, herramientas), herramientas, detector)

    casos = json.loads(CASOS.read_text(encoding="utf-8"))
    filas, aprobados = [], 0
    for caso in casos:
        conversacion = [Mensaje(rol, texto) for rol, texto in caso.get("historial", [])]
        conversacion.append(Mensaje("cliente", caso["pregunta"]))
        inicio = time.perf_counter()
        antes = len(registro_de_casos.registrados)
        try:
            respuesta = asistente.responder(conversacion)
            problemas = revisar(caso, respuesta, len(registro_de_casos.registrados) - antes)
        except Exception as error:  # noqa: BLE001
            respuesta, problemas = Respuesta(""), [f"error: {type(error).__name__}: {error}"]
        segundos = time.perf_counter() - inicio
        aprobados += not problemas
        marca = "✅" if not problemas else "❌"
        print(f"{marca} {caso['id']} ({segundos:.1f} s) {'; '.join(problemas)}")
        if problemas:
            print(f"   respuesta: {respuesta.texto[:300]!r} | herramientas: {respuesta.herramientas_usadas}")
        filas.append(f"| {marca} | {caso['id']} | {segundos:.1f} s | {'; '.join(problemas) or '—'} |")
        time.sleep(PAUSA_SEGUNDOS)

    proporcion = aprobados / len(casos)
    resumen = f"Aprobados: {aprobados} de {len(casos)} ({proporcion:.0%}). Mínimo: {MINIMO:.0%}."
    print(resumen)
    # En GitHub Actions, la tabla aparece en la página de la corrida.
    if salida := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(salida, "a", encoding="utf-8") as archivo:
            archivo.write("## Evaluación del chatbot\n\n| | Caso | Tiempo | Problemas |\n|---|---|---|---|\n")
            archivo.write("\n".join(filas) + f"\n\n**{resumen}**\n")
    return 0 if proporcion >= MINIMO else 1


if __name__ == "__main__":
    sys.exit(main())

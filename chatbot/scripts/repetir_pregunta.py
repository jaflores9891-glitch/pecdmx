"""Hace la misma pregunta varias veces al agente real y muestra qué herramientas usó cada vez.

Sirve para medir la variabilidad del LLM (no siempre decide igual). Usa las mismas piezas en memoria
que la evaluación, así que no necesita base de datos ni backend.
Uso (desde chatbot/):  uv run python scripts/repetir_pregunta.py "¿puedo mandar un perfume?" 8
"""

import sys
import time

import httpx
from evaluar import CasosEnMemoria, PoliticasEnMemoria, backend_de_prueba

from pecdmx_chatbot.agente import Asistente, Mensaje
from pecdmx_chatbot.clasificador import ClasificadorDeQuejas
from pecdmx_chatbot.configuracion import configuracion
from pecdmx_chatbot.embeddings import Incrustador
from pecdmx_chatbot.herramientas import crear_herramientas
from pecdmx_chatbot.llm import crear_llm
from pecdmx_chatbot.negocio import ClienteDeNegocio

pregunta = sys.argv[1]
veces = int(sys.argv[2]) if len(sys.argv) > 2 else 5

config = configuracion()
incrustador = Incrustador(config.modelo_carpeta)
negocio = ClienteDeNegocio("http://backend-de-prueba", transporte=httpx.MockTransport(backend_de_prueba))
herramientas = crear_herramientas(
    negocio, PoliticasEnMemoria(incrustador), CasosEnMemoria(), ClasificadorDeQuejas(incrustador)
)
asistente = Asistente(crear_llm(config, herramientas), herramientas)

for vez in range(1, veces + 1):
    respuesta = asistente.responder([Mensaje("cliente", pregunta)])
    usadas, fuentes = respuesta.herramientas_usadas, len(respuesta.fuentes)
    print(f"{vez}. herramientas={usadas} fuentes={fuentes} | {respuesta.texto[:90]!r}")
    time.sleep(2)

"""Defensas del chatbot contra el abuso: inyección de instrucciones ("prompt injection") y exceso de uso.

Son capas, como las de un banco (guardia, cámara, bóveda); ninguna es perfecta sola:
  1. Límites de tamaño de la conversación (en app.py, al validar la petición).
  2. Reglas propias: frases típicas de ataque, en español y en inglés.
  3. Un modelo detector de Meta (Llama Prompt Guard 2) que corre en Groq.
  4. Instrucciones del agente que le prohíben cambiar sus reglas (agente.py).
  5. Una "palabra trampa" escondida en las instrucciones: si aparece en la respuesta, se filtró el prompt.
  6. Límite de mensajes por persona por minuto.
Además, las herramientas validan sus datos (número de guía, sucursales, contacto)
y el backend vuelve a validar.
"""

import logging
import re
import time
import unicodedata
from collections import defaultdict, deque
from collections.abc import Callable

registro = logging.getLogger("pecdmx_chatbot")

RESPUESTA_A_UN_ATAQUE = (
    "Solo puedo ayudarte con envíos de PECDMX: rastrear una guía, cotizar, resolver dudas "
    "o levantar una queja. ¿En qué te ayudo?"
)

# Frases típicas de ataque. Se comparan sin acentos y en minúsculas.
_PATRONES = [
    # "ignora tus instrucciones", "olvida las reglas"...
    r"\b(ignora|olvida|omite|saltate|desobedece)\b.{0,40}"
    r"\b(instrucciones|reglas|indicaciones|prompt|restricciones)",
    r"\b(ignore|forget|disregard)\b.{0,40}\b(instructions|rules|prompt)",
    # Pedir o fingir el mensaje del sistema.
    r"\b(prompt|instrucciones|mensaje) (de|del) sistema\b",
    r"\bsystem prompt\b",
    r"^\s*(system|sistema|developer|assistant)\s*:",
    # Cambiarle la personalidad: "eres DAN", "actúa como una IA sin filtros".
    r"\b(eres|seras|actua como|finge ser|haz de cuenta que eres)\b.{0,30}"
    r"\b(dan|sin (restricciones|reglas|filtros|limites))\b",
    r"\bmodo (desarrollador|developer|dios|sin restricciones|jailbreak)\b",
    r"\bjailbreak\b",
    # "muéstrame tus instrucciones" (con o sin "-me" al final del verbo).
    r"\b(revela|muestra|repite|imprime|ensena|di|escribe)(me|nos)?\b.{0,30}"
    r"\b(tus|las) (instrucciones|reglas|prompt)",
    # Pedir secretos del sistema.
    r"\b(contrasenas?|passwords?|api ?keys?|llaves?|tokens?|secretos?)\b.{0,40}"
    r"\b(empleados?|gerentes?|sistema|base de datos|servidor)",
]
_ATAQUES = [re.compile(p, re.IGNORECASE | re.MULTILINE) for p in _PATRONES]


def _sin_acentos(texto: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn").lower()


def parece_ataque(texto: str) -> bool:
    """Capa 2: ¿el mensaje tiene frases típicas de inyección de instrucciones?"""
    limpio = _sin_acentos(texto)
    return any(patron.search(limpio) for patron in _ATAQUES)


class DetectorDeInyeccion:
    """Capa 3: Llama Prompt Guard 2 (86M) en Groq. Regresa la probabilidad (0 a 1) de que sea un ataque."""

    MODELO = "meta-llama/llama-prompt-guard-2-86m"
    UMBRAL = 0.5

    def __init__(self, preguntar: Callable[[str], float]) -> None:
        self._preguntar = preguntar

    @classmethod
    def con_groq(cls, api_key: str, espera_segundos: float = 5.0) -> "DetectorDeInyeccion":
        from groq import Groq

        cliente = Groq(api_key=api_key, timeout=espera_segundos, max_retries=0)

        def preguntar(texto: str) -> float:
            respuesta = cliente.chat.completions.create(
                model=cls.MODELO, messages=[{"role": "user", "content": texto}]
            )
            return float(respuesta.choices[0].message.content)

        return cls(preguntar)

    def es_ataque(self, texto: str) -> bool:
        try:
            return self._preguntar(texto) >= self.UMBRAL
        except Exception as error:  # noqa: BLE001
            # Si el detector falla (límite, caída), no se bloquea al cliente: quedan las otras capas.
            registro.warning(
                "El detector de inyección no respondió (%s); se siguió sin él.", type(error).__name__
            )
            return False


def se_filtro_el_prompt(respuesta: str, palabra_trampa: str) -> bool:
    """Capa 5: la palabra trampa solo existe en las instrucciones; si sale en la respuesta, se filtraron."""
    return palabra_trampa.lower() in respuesta.lower()


class LimiteDeMensajes:
    """Capa 6: cuántos mensajes puede mandar una misma persona (por su IP) en una ventana de tiempo.

    Vive en la memoria del servidor: en el plan gratis hay un solo servidor, así que basta.
    """

    def __init__(
        self, maximo: int = 10, ventana_segundos: float = 60.0, reloj: Callable[[], float] = time.monotonic
    ):
        self._maximo = maximo
        self._ventana = ventana_segundos
        self._reloj = reloj
        self._historial: dict[str, deque[float]] = defaultdict(deque)

    def permitir(self, quien: str) -> bool:
        ahora = self._reloj()
        recientes = self._historial[quien]
        while recientes and ahora - recientes[0] >= self._ventana:
            recientes.popleft()
        if len(recientes) >= self._maximo:
            return False
        recientes.append(ahora)
        # Limpieza: que la memoria no crezca con IPs que ya no escriben.
        if len(self._historial) > 10_000:
            viejas = [ip for ip, t in self._historial.items() if not t or ahora - t[-1] >= self._ventana]
            for ip in viejas:
                del self._historial[ip]
        return True

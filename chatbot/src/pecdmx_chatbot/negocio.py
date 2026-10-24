"""Cliente del backend de negocio (Java): rastrear guías y cotizar envíos.

El chatbot nunca inventa estados ni precios: siempre se los pregunta al backend,
que es la única fuente de verdad.
"""

import re
from typing import Any

import httpx

# El mismo formato que valida el backend (PEC + 10 dígitos). Revisarlo aquí evita llamadas inútiles
# y que el LLM meta en la dirección algo que no es un número de guía.
FORMATO_DE_GUIA = re.compile(r"PEC\d{10}")


class BackendNoDisponibleError(RuntimeError):
    """El backend no respondió (dormido, caído o sin internet)."""


class ErrorDelNegocio(ValueError):
    """El backend rechazó la petición (guía que no existe, peso de más...). Trae su mensaje en español."""


class ClienteDeNegocio:
    def __init__(
        self, url_base: str, espera_segundos: float = 90.0, transporte: httpx.BaseTransport | None = None
    ) -> None:
        # Espera larga: en el plan gratis de Render el backend puede tardar ~1.5 min en despertar.
        self._http = httpx.Client(
            base_url=url_base.rstrip("/"), timeout=espera_segundos, transport=transporte
        )

    def rastrear(self, numero: str) -> dict[str, Any]:
        numero = numero.strip().upper().replace(" ", "").replace("-", "")
        if not FORMATO_DE_GUIA.fullmatch(numero):
            raise ErrorDelNegocio("El número de guía debe ser las letras PEC seguidas de 10 dígitos.")
        return self._pedir("GET", f"/api/guias/{numero}")

    def cotizar(
        self, origen: str, destino: str, peso_kg: float, largo_cm: int, ancho_cm: int, alto_cm: int
    ) -> dict[str, Any]:
        cuerpo = {
            "origen": origen,
            "destino": destino,
            "pesoKg": peso_kg,
            "largoCm": largo_cm,
            "anchoCm": ancho_cm,
            "altoCm": alto_cm,
        }
        return self._pedir("POST", "/api/cotizaciones", json=cuerpo)

    def _pedir(self, metodo: str, ruta: str, **opciones: Any) -> dict[str, Any]:
        try:
            respuesta = self._http.request(metodo, ruta, **opciones)
        except httpx.HTTPError as error:
            raise BackendNoDisponibleError("El sistema de PECDMX no respondió.") from error
        if respuesta.status_code >= 500:
            raise BackendNoDisponibleError(f"El sistema de PECDMX falló ({respuesta.status_code}).")
        if respuesta.is_error:
            raise ErrorDelNegocio(_mensaje_de_error(respuesta))
        return respuesta.json()


def _mensaje_de_error(respuesta: httpx.Response) -> str:
    """El mensaje del backend (Problem Details): primero la lista "errores", luego "detail"."""
    try:
        problema = respuesta.json()
    except ValueError:
        return "La petición no es válida."
    if problema.get("errores"):
        return " ".join(problema["errores"])
    return problema.get("detail") or "La petición no es válida."

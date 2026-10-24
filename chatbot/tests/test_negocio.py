"""El cliente del backend, con un backend falso (httpx.MockTransport): sin red ni Java."""

import json

import httpx
import pytest
from falsos import COTIZACION, RASTREO, backend_falso, problema

from pecdmx_chatbot.negocio import BackendNoDisponibleError, ErrorDelNegocio


def test_rastrear_limpia_el_numero_y_consulta_la_ruta_publica():
    rutas = []

    def responder(peticion: httpx.Request) -> httpx.Response:
        rutas.append(peticion.url.path)
        return httpx.Response(200, json=RASTREO)

    assert backend_falso(responder).rastrear(" pec 0000-000018 ") == RASTREO
    assert rutas == ["/api/guias/PEC0000000018"]


@pytest.mark.parametrize("numero", ["123", "PEC123", "../api/usuarios", "PEC0000000018/detalle"])
def test_un_numero_con_formato_invalido_ni_siquiera_llega_al_backend(numero):
    def responder(peticion: httpx.Request) -> httpx.Response:
        raise AssertionError("no debió llamar al backend")

    with pytest.raises(ErrorDelNegocio, match="PEC"):
        backend_falso(responder).rastrear(numero)


def test_cotizar_manda_los_nombres_que_espera_el_backend():
    cuerpos = []

    def responder(peticion: httpx.Request) -> httpx.Response:
        cuerpos.append(json.loads(peticion.content))
        return httpx.Response(200, json=COTIZACION)

    backend_falso(responder).cotizar("CDMX", "GDL", 3.5, 40, 30, 20)

    assert cuerpos == [
        {"origen": "CDMX", "destino": "GDL", "pesoKg": 3.5, "largoCm": 40, "anchoCm": 30, "altoCm": 20}
    ]


def test_el_mensaje_del_backend_llega_tal_cual():
    backend = backend_falso(lambda _: problema(404, detail="No existe la guía PEC0000000026."))

    with pytest.raises(ErrorDelNegocio, match="No existe la guía PEC0000000026"):
        backend.rastrear("PEC0000000026")


def test_la_lista_de_errores_de_validacion_se_junta():
    backend = backend_falso(
        lambda _: problema(400, detail="Inválida", errores=["Falta el peso.", "Falta el alto."])
    )

    with pytest.raises(ErrorDelNegocio, match="Falta el peso. Falta el alto."):
        backend.cotizar("CDMX", "GDL", 0, 1, 1, 1)


def test_un_error_500_es_backend_no_disponible():
    with pytest.raises(BackendNoDisponibleError):
        backend_falso(lambda _: httpx.Response(502)).rastrear("PEC0000000018")


def test_sin_conexion_es_backend_no_disponible():
    def responder(peticion: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("sin red")

    with pytest.raises(BackendNoDisponibleError):
        backend_falso(responder).rastrear("PEC0000000018")

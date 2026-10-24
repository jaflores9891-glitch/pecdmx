"""Lo que cada herramienta le entrega al LLM: textos claros, en hora de México y sin lanzar errores."""

import httpx
from falsos import ConocimientoFalso, backend_falso, backend_que_sabe_todo, problema

from pecdmx_chatbot.herramientas import SIN_SISTEMA, crear_herramientas


def herramientas(backend=None) -> dict:
    return {h.name: h for h in crear_herramientas(backend or backend_que_sabe_todo(), ConocimientoFalso())}


def test_el_rastreo_trae_estado_e_historial_en_hora_del_centro_de_mexico():
    texto = herramientas()["rastrear_guia"].invoke({"numero": "PEC0000000018"})

    assert "Estado actual: En tránsito — Va en camino." in texto
    assert "de Ciudad de México (CDMX) a Monterrey (MTY)" in texto
    # 15:00 UTC = 09:00 en CDMX (UTC-6, sin horario de verano desde 2022).
    assert "20/10/2026 09:00: Registrada en Ciudad de México (CDMX)" in texto
    # 02:30 UTC del 21 = 20:30 del 20 en CDMX: también cambia el día.
    assert (
        "20/10/2026 20:30: En tránsito en Ciudad de México (CDMX) (Salió en el camión de la noche)" in texto
    )


def test_si_la_guia_no_existe_lo_dice_sin_lanzar_error():
    texto = herramientas()["rastrear_guia"].invoke({"numero": "PEC0000000026"})

    assert texto == "No se pudo rastrear: No existe la guía."


def test_la_cotizacion_trae_el_desglose_completo():
    texto = herramientas()["cotizar_envio"].invoke(
        {"origen": "CDMX", "destino": "GDL", "peso_kg": 3.5, "largo_cm": 40, "ancho_cm": 30, "alto_cm": 20}
    )

    assert "se cobran 5 kg" in texto
    assert "total $359.6 MXN" in texto


def test_si_el_backend_rechaza_la_cotizacion_explica_por_que():
    backend = backend_falso(lambda _: problema(422, detail="El peso máximo es 70 kg."))

    texto = herramientas(backend)["cotizar_envio"].invoke(
        {"origen": "CDMX", "destino": "GDL", "peso_kg": 90, "largo_cm": 40, "ancho_cm": 30, "alto_cm": 20}
    )

    assert texto == "No se pudo cotizar: El peso máximo es 70 kg."


def test_si_el_backend_esta_caido_pide_intentar_mas_tarde():
    backend = backend_falso(lambda _: httpx.Response(503))

    assert herramientas(backend)["rastrear_guia"].invoke({"numero": "PEC0000000018"}) == SIN_SISTEMA


def test_las_sucursales_solo_pueden_ser_las_6_de_pecdmx():
    esquema = herramientas()["cotizar_envio"].args["origen"]

    assert esquema["enum"] == ["CDMX", "GDL", "MTY", "PUE", "QRO", "TIJ"]


def test_buscar_politicas_da_cada_fragmento_con_su_fuente():
    llamada = {"name": "buscar_politicas", "args": {"pregunta": "¿pilas?"}, "id": "1", "type": "tool_call"}

    mensaje = herramientas()["buscar_politicas"].invoke(llamada)

    assert "[Fuente: Artículos prohibidos › Pilas]\nNo se envían pilas sueltas." in mensaje.content
    assert mensaje.artifact == ["Artículos prohibidos › Pilas", "Embalaje › Frágiles"]

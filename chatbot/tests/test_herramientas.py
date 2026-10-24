"""Lo que cada herramienta le entrega al LLM: textos claros, en hora de México y sin lanzar errores."""

import httpx
import pytest
from falsos import CasosFalsos, backend_falso, herramientas_falsas, problema

from pecdmx_chatbot.herramientas import SIN_SISTEMA


def herramientas(backend=None, casos=None) -> dict:
    return {h.name: h for h in herramientas_falsas(backend, casos)}


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


def test_una_queja_se_clasifica_y_se_guarda_con_la_sucursal_de_destino():
    casos = CasosFalsos()

    texto = herramientas(casos=casos)["levantar_queja"].invoke(
        {"numero_guia": "pec0000000018", "descripcion": "llegó roto el vaso", "contacto": "ana@correo.mx"}
    )

    assert texto.startswith("Queja registrada con folio Q-000001. Tipo: Daño.")
    [caso] = casos.registrados
    assert (caso.tipo, caso.guia, caso.sucursal, caso.categoria) == ("QUEJA", "PEC0000000018", "MTY", "DANO")


def test_no_se_levanta_queja_de_una_guia_que_no_existe():
    casos = CasosFalsos()

    texto = herramientas(casos=casos)["levantar_queja"].invoke(
        {"numero_guia": "PEC0000000026", "descripcion": "no llega", "contacto": "5512345678"}
    )

    assert texto == "No se pudo levantar la queja: No existe la guía."
    assert casos.registrados == []


def test_si_el_backend_no_responde_la_queja_se_guarda_sin_sucursal():
    casos = CasosFalsos()
    backend = backend_falso(lambda _: httpx.Response(503))

    herramientas(backend, casos)["levantar_queja"].invoke(
        {"numero_guia": "PEC0000000018", "descripcion": "no llega", "contacto": "5512345678"}
    )

    assert casos.registrados[0].sucursal is None


@pytest.mark.parametrize("contacto", ["", "mañana te digo", "55 1234", "ana@correo"])
def test_sin_un_contacto_valido_no_se_registra_nada(contacto):
    casos = CasosFalsos()
    tools = herramientas(casos=casos)

    queja = tools["levantar_queja"].invoke(
        {"numero_guia": "PEC0000000018", "descripcion": "no llega", "contacto": contacto}
    )
    atencion = tools["pasar_a_empleado"].invoke({"motivo": "quiero factura", "contacto": contacto})

    assert queja.startswith("Falta un contacto válido") and atencion.startswith("Falta un contacto válido")
    assert casos.registrados == []


def test_pasar_a_empleado_crea_un_caso_de_atencion_con_folio():
    casos = CasosFalsos()

    texto = herramientas(casos=casos)["pasar_a_empleado"].invoke(
        {"motivo": "quiere factura de su envío", "contacto": "(55) 1234-5678"}
    )

    assert texto.startswith("Caso pasado a un empleado con folio A-000001.")
    assert casos.registrados[0].tipo == "ATENCION" and casos.registrados[0].guia is None

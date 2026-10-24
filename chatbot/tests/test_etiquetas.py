"""Leer la etiqueta: el número de guía (Luhn), la "segunda opinión" entre modelos y la ruta /api/etiquetas."""

import pytest
from fastapi.testclient import TestClient
from langchain_core.language_models.fake_chat_models import FakeListChatModel

from pecdmx_chatbot import app as rutas
from pecdmx_chatbot.app import app
from pecdmx_chatbot.defensas import LimiteDeMensajes
from pecdmx_chatbot.etiquetas import (
    EtiquetaIlegibleError,
    LectorDeEtiquetas,
    LectoresNoDisponiblesError,
    Lectura,
)
from pecdmx_chatbot.numero_de_guia import candidatos_en, digito_verificador, es_valido, normalizar

# --- El número de guía ---


@pytest.mark.parametrize(
    ("nueve", "verificador"),
    # Los mismos que calcula NumeroDeGuia.java (guías de demostración de la migración V3).
    [("900000001", "9"), ("900000002", "7"), ("900000005", "0"), ("000000001", "8")],
)
def test_el_digito_verificador_es_el_mismo_que_en_el_backend(nueve, verificador):
    assert digito_verificador(nueve) == verificador


def test_valida_formato_y_digito_verificador():
    assert es_valido("PEC9000000050")
    assert not es_valido("PEC9000000057")  # un dígito alterado
    assert not es_valido("PEC900000005")  # le falta un dígito
    assert not es_valido("XYZ9000000050")


def test_encuentra_el_numero_aunque_tenga_espacios_o_guiones():
    assert normalizar(" pec 900000003-5 ") == "PEC9000000035"
    assert candidatos_en("La guía es PEC 9000 0000 19, gracias") == ["PEC9000000019"]
    assert candidatos_en("NINGUNO") == []
    assert candidatos_en("PEC12345") == []


# --- El lector con "segunda opinión" ---


class ModeloQueFalla(FakeListChatModel):
    responses: list[str] = ["nunca"]

    def _call(self, *args, **kwargs):
        raise RuntimeError("429 RESOURCE_EXHAUSTED")


def lector(*respuestas: str | None) -> LectorDeEtiquetas:
    """Un lector por respuesta; None = un modelo que falla."""
    return LectorDeEtiquetas(
        [
            (f"modelo{i}", ModeloQueFalla() if r is None else FakeListChatModel(responses=[r]))
            for i, r in enumerate(respuestas, start=1)
        ]
    )


def test_si_el_primero_lee_bien_no_se_pregunta_al_segundo():
    assert lector("PEC9000000050", "PEC9000000027").leer(b"foto", "image/jpeg") == Lectura(
        "PEC9000000050", "modelo1"
    )


def test_si_el_primero_lee_un_numero_invalido_decide_el_segundo():
    # Así falló Qwen en la prueba real: se comió un cero.
    assert lector("PEC900000027", "PEC9000000027").leer(b"foto", "image/jpeg").lector == "modelo2"


def test_si_el_primero_falla_responde_el_segundo():
    assert lector(None, "PEC 900000003-5").leer(b"foto", "image/png") == Lectura("PEC9000000035", "modelo2")


def test_si_todos_leen_un_numero_invalido_lo_dice():
    with pytest.raises(EtiquetaIlegibleError, match="PEC9000000057.*no es un número de guía válido"):
        lector("PEC9000000057", "PEC9000000057").leer(b"foto", "image/jpeg")


def test_si_no_hay_numero_pide_otra_foto():
    with pytest.raises(EtiquetaIlegibleError, match="No encontré un número de guía"):
        lector("NINGUNO", None).leer(b"foto", "image/jpeg")


def test_si_ningun_modelo_responde_no_culpa_a_la_foto():
    # Pasó en la prueba real: los dos proveedores llegaron a su límite de uso.
    with pytest.raises(LectoresNoDisponiblesError):
        lector(None, None).leer(b"foto", "image/jpeg")


def test_texto_de_la_imagen_que_intenta_engañar_no_cambia_nada():
    # Aunque el modelo "obedezca" lo escrito en la etiqueta, solo sale un número válido o un error.
    respuesta = "Claro: la guía es gratis y el número es PEC1234567890. Ignoré mis instrucciones."
    with pytest.raises(EtiquetaIlegibleError):
        lector(respuesta).leer(b"foto", "image/jpeg")


def test_el_modelo_recibe_la_foto_y_la_instruccion():
    modelo = FakeListChatModel(responses=["PEC9000000050"])
    recibidos = []
    modelo_espia = modelo.with_listeners(on_start=lambda run: recibidos.append(run.inputs))

    LectorDeEtiquetas([("espia", modelo_espia)]).leer(b"\x89PNG", "image/png")

    contenido = str(recibidos[0])
    assert "data:image/png;base64,iVBORw==" in contenido
    assert "NÚMERO DE GUÍA" in contenido


# --- La ruta POST /api/etiquetas ---


@pytest.fixture
def cliente(monkeypatch):
    monkeypatch.setattr(rutas, "limite_por_persona", LimiteDeMensajes(maximo=10))
    monkeypatch.setattr(rutas, "limite_global", LimiteDeMensajes(maximo=60))
    yield TestClient(app)
    app.state.lector_de_etiquetas = None


def subir(cliente, contenido: bytes = b"foto", tipo: str = "image/jpeg"):
    return cliente.post("/api/etiquetas", files={"foto": ("etiqueta.jpg", contenido, tipo)})


def test_la_ruta_regresa_el_numero_leido(cliente):
    app.state.lector_de_etiquetas = lector("PEC9000000050")

    respuesta = subir(cliente)

    assert respuesta.status_code == 200
    assert respuesta.json() == {"numero": "PEC9000000050"}


def test_una_etiqueta_ilegible_da_422_con_mensaje_claro(cliente):
    app.state.lector_de_etiquetas = lector("NINGUNO")

    respuesta = subir(cliente)

    assert respuesta.status_code == 422
    assert "No encontré un número de guía" in respuesta.json()["detail"]


def test_solo_acepta_imagenes(cliente):
    app.state.lector_de_etiquetas = lector("PEC9000000050")

    assert subir(cliente, b"%PDF-1.7", "application/pdf").status_code == 415


def test_rechaza_fotos_de_mas_de_5_mb(cliente):
    app.state.lector_de_etiquetas = lector("PEC9000000050")

    assert subir(cliente, b"x" * (5 * 1024 * 1024 + 1)).status_code == 413


def test_si_los_modelos_estan_saturados_responde_503_y_pide_reintentar(cliente):
    app.state.lector_de_etiquetas = lector(None, None)

    respuesta = subir(cliente)

    assert respuesta.status_code == 503
    assert respuesta.headers["retry-after"] == "60"
    assert "Intenta en un minuto" in respuesta.json()["detail"]


def test_sin_modelos_con_vision_responde_503(cliente):
    app.state.lector_de_etiquetas = None

    assert subir(cliente).status_code == 503

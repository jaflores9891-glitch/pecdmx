from pathlib import Path

import pytest

from pecdmx_chatbot.politicas import Fragmento, huella, leer_politica, leer_politicas


def test_parte_una_politica_en_un_fragmento_por_seccion(tmp_path: Path):
    ruta = tmp_path / "prueba.md"
    ruta.write_text(
        "# Política de prueba\n\n## Primera sección\nTexto   uno,\ncon salto.\n\n## Segunda\nTexto dos.\n",
        encoding="utf-8",
    )

    fragmentos = leer_politica(ruta)

    assert fragmentos == [
        Fragmento("prueba.md", "Política de prueba", "Primera sección", "Texto uno, con salto."),
        Fragmento("prueba.md", "Política de prueba", "Segunda", "Texto dos."),
    ]
    assert fragmentos[0].cita == "Política de prueba › Primera sección"


def test_una_politica_sin_titulo_da_un_error_claro(tmp_path: Path):
    ruta = tmp_path / "sin-titulo.md"
    ruta.write_text("## Sección\nTexto.\n", encoding="utf-8")

    with pytest.raises(ValueError, match="no tiene título"):
        leer_politica(ruta)


def test_la_huella_cambia_si_cambia_cualquier_politica():
    original = [Fragmento("a.md", "A", "Uno", "texto")]
    cambiada = [Fragmento("a.md", "A", "Uno", "texto distinto")]

    assert huella(original) == huella(list(original))
    assert huella(original) != huella(cambiada)


def test_las_politicas_reales_estan_bien_escritas(configuracion_de_prueba):
    fragmentos = leer_politicas(configuracion_de_prueba.politicas_carpeta)

    assert len({f.archivo for f in fragmentos}) == 6
    assert len(fragmentos) >= 20
    # Cada sección con suficiente texto para responder y no tan larga que el modelo la recorte.
    for f in fragmentos:
        assert 60 <= len(f.contenido) <= 1500, f.cita
    # Las citas no se repiten: cada una apunta a un solo lugar.
    assert len({f.cita for f in fragmentos}) == len(fragmentos)

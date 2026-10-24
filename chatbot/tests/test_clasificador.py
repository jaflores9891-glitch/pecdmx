"""El clasificador de quejas con sus pesos reales (los que se publican) y el modelo de embeddings real."""

import csv
from pathlib import Path

import numpy as np
import pytest

from pecdmx_chatbot.clasificador import CATEGORIAS, ClasificadorDeQuejas
from pecdmx_chatbot.embeddings import DIMENSIONES

DATOS = Path(__file__).resolve().parents[1] / "datos" / "quejas.csv"


@pytest.fixture(scope="module")
def clasificador(incrustador_compartido) -> ClasificadorDeQuejas:
    return ClasificadorDeQuejas(incrustador_compartido)


def test_hay_suficientes_ejemplos_de_cada_tipo():
    with DATOS.open(encoding="utf-8") as archivo:
        categorias = [fila["categoria"] for fila in csv.DictReader(archivo)]

    for categoria in CATEGORIAS:
        assert categorias.count(categoria) >= 30, categoria
    assert set(categorias) == set(CATEGORIAS)


def test_los_pesos_tienen_la_forma_correcta():
    with np.load(Path(__file__).resolve().parents[1] / "src/pecdmx_chatbot/clasificador_quejas.npz") as datos:
        assert datos["w"].shape == (len(CATEGORIAS), DIMENSIONES)
        assert datos["b"].shape == (len(CATEGORIAS),)


def test_los_pesos_guardados_corresponden_a_los_ejemplos(clasificador):
    # Si alguien cambia quejas.csv y olvida reentrenar, este test lo detecta.
    with DATOS.open(encoding="utf-8") as archivo:
        filas = list(csv.DictReader(archivo))

    aciertos = sum(clasificador.clasificar(f["texto"]).categoria == f["categoria"] for f in filas)

    assert aciertos / len(filas) >= 0.95


def test_aprueba_el_examen_con_quejas_que_nunca_vio(clasificador):
    # datos/quejas_examen.csv NO se usa para entrenar. Un modelo estadístico siempre falla en algunos casos
    # (ej. "partida en dos"), por eso se exige un porcentaje y no que acierte cada frase.
    with (DATOS.parent / "quejas_examen.csv").open(encoding="utf-8") as archivo:
        examen = list(csv.DictReader(archivo))

    fallos = [
        (f["texto"], f["categoria"], clasificador.clasificar(f["texto"]).categoria)
        for f in examen
        if clasificador.clasificar(f["texto"]).categoria != f["categoria"]
    ]
    acierto = 1 - len(fallos) / len(examen)

    print(f"Acierto en el examen: {acierto:.0%}. Fallos: {fallos}")
    assert acierto >= 0.8, fallos


def test_ninguna_queja_del_examen_se_uso_para_entrenar():
    with (
        DATOS.open(encoding="utf-8") as entrenamiento,
        (DATOS.parent / "quejas_examen.csv").open(encoding="utf-8") as examen,
    ):
        vistas = {f["texto"] for f in csv.DictReader(entrenamiento)}
        assert not vistas & {f["texto"] for f in csv.DictReader(examen)}


def test_las_probabilidades_suman_uno(clasificador):
    probabilidades = clasificador.probabilidades("mi paquete llegó roto")

    assert set(probabilidades) == set(CATEGORIAS)
    assert sum(probabilidades.values()) == pytest.approx(1.0)


def test_si_no_esta_seguro_lo_manda_a_otro_para_que_lo_revise_una_persona(incrustador_compartido, tmp_path):
    # Pesos en cero: las 4 categorías empatan con 25 % cada una, por debajo del mínimo de confianza.
    pesos = tmp_path / "pesos.npz"
    np.savez(pesos, w=np.zeros((len(CATEGORIAS), DIMENSIONES)), b=np.zeros(len(CATEGORIAS)))

    clasificacion = ClasificadorDeQuejas(incrustador_compartido, pesos).clasificar("mi paquete llegó roto")

    assert clasificacion.categoria == "OTRO"
    assert clasificacion.confianza == pytest.approx(0.25)

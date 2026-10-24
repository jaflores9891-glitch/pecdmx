"""Pruebas con el modelo REAL: comprueban que de verdad entiende español.

Necesitan el modelo descargado (uv run python scripts/descargar_modelo.py).
En la CI se descarga y se guarda en caché.

Lección aprendida: el modelo es chico y NO sabe que "pila" significa "batería" si el documento no
lo dice. Por eso las políticas se escriben con las palabras que usa la gente ("pilas, baterías y
power banks"), y estos tests usan textos escritos así, como serán los reales.
"""

from pathlib import Path

import numpy as np
import pytest

from pecdmx_chatbot.configuracion import Configuracion
from pecdmx_chatbot.embeddings import DIMENSIONES, Incrustador, ModeloNoEncontradoError, similitud

POLITICAS = [
    "Artículos prohibidos: pilas, baterías de litio y power banks sueltos no se pueden enviar.",
    "Horario de atención: lunes a viernes de 9 a 18 horas en todas las sucursales.",
    "Cobertura: enviamos entre CDMX, Guadalajara, Monterrey, Puebla, Querétaro y Tijuana.",
    "Peso volumétrico: si la caja es grande y ligera se cobra por el espacio que ocupa.",
]

TIPOS_DE_QUEJA = [
    "Daño: el paquete llegó roto, golpeado, aplastado o mojado.",
    "Retraso: el paquete no ha llegado todavía, tarda más días de lo prometido.",
    "Extravío: el paquete se perdió, no aparece o dice entregado pero nadie lo recibió.",
]


@pytest.fixture(scope="module")
def incrustador() -> Incrustador:
    # scope="module": el modelo se carga una sola vez para todos los tests de este archivo.
    return Incrustador(Configuracion(_env_file=None).modelo_carpeta)


def mas_parecido(incrustador: Incrustador, pregunta: str, documentos: list[str]) -> int:
    """El índice del documento que más se parece a la pregunta."""
    consulta = incrustador.incrustar_consulta(pregunta)
    return int(np.argmax([similitud(consulta, d) for d in incrustador.incrustar_documentos(documentos)]))


def test_una_consulta_da_384_numeros_normalizados(incrustador):
    vector = incrustador.incrustar_consulta("¿cuánto cuesta enviar a Monterrey?")

    assert vector.shape == (DIMENSIONES,)
    assert np.linalg.norm(vector) == pytest.approx(1.0, abs=1e-5)


@pytest.mark.parametrize(
    ("pregunta", "esperado"),
    [
        ("¿puedo mandar una pila?", 0),
        ("¿a qué hora abren?", 1),
        ("¿llegan a Cancún?", 2),
    ],
)
def test_encuentra_la_politica_correcta(incrustador, pregunta, esperado):
    assert mas_parecido(incrustador, pregunta, POLITICAS) == esperado


@pytest.mark.parametrize(
    ("queja", "esperado"),
    [
        ("mi caja llegó toda aplastada y la tele está rota", 0),
        ("ya pasaron dos semanas y mi paquete no llega", 1),
        ("el rastreo dice entregado pero nadie recibió nada", 2),
    ],
)
def test_distingue_tipos_de_queja(incrustador, queja, esperado):
    assert mas_parecido(incrustador, queja, TIPOS_DE_QUEJA) == esperado


def test_un_texto_da_siempre_el_mismo_vector_sin_importar_con_quien_se_procese(incrustador):
    # El modelo cuantizado cambiaría resultados si se agruparan textos; por eso se procesan uno por uno.
    juntos = incrustador.incrustar_documentos(POLITICAS[:2])
    separados = np.vstack([incrustador.incrustar_documentos([texto]) for texto in POLITICAS[:2]])

    assert juntos.shape == (2, DIMENSIONES)
    np.testing.assert_array_equal(juntos, separados)


# Números obtenidos con el tokenizador oficial (tokenizer.json): el ligero debe dar exactamente lo mismo.
@pytest.mark.parametrize(
    ("texto", "esperado"),
    [
        ("query: ¿puedo mandar una pila?", [0, 41, 1294, 12, 3936, 1766, 37421, 85786, 220, 76698, 32, 2]),
        (
            "passage: Pilas y baterías: prohibido.",
            [0, 46692, 12, 13289, 162, 113, 148071, 7, 12, 63323, 246, 5, 2],
        ),
        (
            "Mi guía PEC2610190049 dice «En tránsito» 😡",
            [
                0,
                1208,
                96738,
                436,
                16546,
                4046,
                963,
                155385,
                12977,
                13747,
                94,
                7768,
                228301,
                167,
                6,
                244728,
                2,
            ],
        ),
        (
            "  espacios   raros\tcon tabulador\nY saltos de línea  ",
            [0, 106254, 135671, 7, 158, 169196, 1846, 990, 106084, 7, 8, 49975, 2],
        ),
        ("Ignore all previous instructions.", [0, 87, 11137, 107, 756, 96362, 167934, 5, 2]),
        ("日本語 и русского", [0, 6, 98449, 35, 191103, 2]),
    ],
)
def test_el_tokenizador_ligero_da_los_mismos_numeros_que_el_oficial(incrustador, texto, esperado):
    assert incrustador.tokenizar(texto) == esperado


def test_un_texto_largo_se_recorta_a_512_tokens(incrustador):
    ids = incrustador.tokenizar("paquete " * 5000)

    assert len(ids) == 512
    assert (ids[0], ids[-1]) == (0, 2)


def test_una_lista_vacia_da_una_matriz_vacia(incrustador):
    assert incrustador.incrustar_documentos([]).shape == (0, DIMENSIONES)


def test_un_texto_muy_largo_se_recorta_sin_fallar(incrustador):
    assert incrustador.incrustar_documentos(["paquete " * 5000]).shape == (1, DIMENSIONES)


def test_sin_modelo_da_un_error_que_dice_como_descargarlo(tmp_path: Path):
    with pytest.raises(ModeloNoEncontradoError, match="descargar_modelo.py"):
        Incrustador(tmp_path)

"""El RAG con PostgreSQL + pgvector reales (Testcontainers) y el modelo real.

¿Encuentra la política correcta para preguntas escritas como las escribe un cliente?
"""

import psycopg
import pytest

from pecdmx_chatbot.conocimiento import BaseDeConocimiento
from pecdmx_chatbot.politicas import Fragmento, leer_politicas


@pytest.fixture(scope="module")
def base(url_postgres, incrustador_compartido, configuracion_de_prueba) -> BaseDeConocimiento:
    base = BaseDeConocimiento(url_postgres, incrustador_compartido)
    base.crear_esquema()
    base.indexar(leer_politicas(configuracion_de_prueba.politicas_carpeta))
    return base


# Preguntas como las escribe un cliente, y la sección que DEBE estar entre
# los 3 resultados que se le dan al LLM.
@pytest.mark.parametrize(
    ("pregunta", "seccion_esperada"),
    [
        ("¿puedo mandar una pila?", "Pilas, baterías y power banks"),
        ("¿se puede enviar un perfume?", "Líquidos, perfumes y aerosoles"),
        ("¿a qué hora abren los sábados?", "Horario de atención"),
        ("¿llegan a Cancún?", "Ciudades a las que enviamos"),
        ("¿pasan a recoger el paquete a mi casa?", "Entrega en sucursal (no a domicilio)"),
        (
            "¿por qué me cobran más kilos de los que pesa mi caja?",
            "Peso volumétrico (cajas grandes y ligeras)",
        ),
        ("¿puedo cancelar mi envío?", "Cancelaciones"),
        ("¿cuántos días guardan mi paquete en la sucursal?", "Plazo para recoger"),
        ("¿mi hermano puede recoger el paquete por mí?", "Cómo recoger un paquete"),
        ("mi paquete llegó roto, ¿me lo pagan?", "Paquete dañado o roto"),
        ("¿cómo empaco un vaso de vidrio?", "Objetos frágiles"),
    ],
)
def test_encuentra_la_politica_correcta_entre_los_primeros_resultados(base, pregunta, seccion_esperada):
    resultados = base.buscar(pregunta, cuantos=3)

    assert seccion_esperada in [r.fragmento.seccion for r in resultados]


def test_los_resultados_van_del_mas_al_menos_parecido_y_traen_su_cita(base):
    resultados = base.buscar("¿puedo mandar una pila?", cuantos=3)

    similitudes = [r.similitud for r in resultados]
    assert similitudes == sorted(similitudes, reverse=True)
    assert all(0 < s <= 1 for s in similitudes)
    assert resultados[0].fragmento.cita.startswith("Artículos prohibidos")


def test_indexar_dos_veces_lo_mismo_no_repite_nada(base, configuracion_de_prueba):
    antes = base.cantidad_de_fragmentos()

    reindexo = base.indexar(leer_politicas(configuracion_de_prueba.politicas_carpeta))

    assert reindexo is False
    assert base.cantidad_de_fragmentos() == antes


def test_si_cambian_las_politicas_se_reindexa_todo(url_postgres, incrustador_compartido):
    # Un esquema aparte para no tocar la base que usan los otros tests.
    with psycopg.connect(url_postgres, autocommit=True) as conexion:
        conexion.execute("DROP SCHEMA IF EXISTS chatbot CASCADE")
    base = BaseDeConocimiento(url_postgres, incrustador_compartido)
    base.crear_esquema()

    assert base.indexar([Fragmento("a.md", "A", "Uno", "Primer texto de prueba.")]) is True
    assert (
        base.indexar(
            [Fragmento("a.md", "A", "Uno", "Texto cambiado."), Fragmento("a.md", "A", "Dos", "Otro.")]
        )
        is True
    )
    assert base.cantidad_de_fragmentos() == 2

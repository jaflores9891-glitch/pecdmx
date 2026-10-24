"""El repositorio de casos con un PostgreSQL real (Testcontainers)."""

import psycopg
import pytest

from pecdmx_chatbot.casos import CasoNoEncontradoError, CasoNuevo, RepositorioDeCasos


@pytest.fixture
def casos(url_postgres) -> RepositorioDeCasos:
    repositorio = RepositorioDeCasos(url_postgres)
    repositorio.crear_esquema()
    # Cada test empieza con la tabla vacía y los folios desde 1.
    with psycopg.connect(url_postgres, autocommit=True) as conexion:
        conexion.execute("TRUNCATE chatbot.casos RESTART IDENTITY")
    return repositorio


def queja(sucursal: str | None = "MTY", descripcion: str = "llegó roto") -> CasoNuevo:
    return CasoNuevo(
        "QUEJA", descripcion, "ana@correo.mx", "PEC0000000018", sucursal, categoria="DANO", confianza=0.93
    )


def test_cada_tipo_de_caso_tiene_su_folio_consecutivo(casos):
    assert casos.registrar(queja()) == "Q-000001"
    assert casos.registrar(CasoNuevo("ATENCION", "quiere factura", "5512345678")) == "A-000002"


def test_se_guarda_todo_lo_del_caso(casos):
    folio = casos.registrar(queja())

    caso = casos.buscar(folio)

    assert (caso.tipo, caso.categoria, caso.guia, caso.sucursal) == ("QUEJA", "DANO", "PEC0000000018", "MTY")
    assert caso.confianza == pytest.approx(0.93)
    assert caso.estado == "ABIERTO" and caso.cerrado_por is None


def test_un_empleado_ve_su_sucursal_y_los_casos_sin_sucursal(casos):
    casos.registrar(queja("MTY", "de Monterrey"))
    casos.registrar(queja("GDL", "de Guadalajara"))
    casos.registrar(queja(None, "sin sucursal"))

    de_monterrey = [c.descripcion for c in casos.listar("ABIERTO", "MTY")]
    todos = [c.descripcion for c in casos.listar("ABIERTO", None)]

    assert de_monterrey == ["de Monterrey", "sin sucursal"]
    assert todos == ["de Monterrey", "de Guadalajara", "sin sucursal"]


def test_cerrar_guarda_quien_y_que_respondio(casos):
    folio = casos.registrar(queja())

    cerrado = casos.cerrar(folio, "empleado@pecdmx.mx", "Se reembolsó el envío.")

    assert cerrado.estado == "CERRADO"
    assert (cerrado.cerrado_por, cerrado.respuesta) == ("empleado@pecdmx.mx", "Se reembolsó el envío.")
    assert cerrado.cerrado_en is not None
    assert casos.listar("ABIERTO") == []
    assert [c.folio for c in casos.listar("CERRADO")] == [folio]


def test_no_se_puede_cerrar_dos_veces_ni_un_folio_que_no_existe(casos):
    folio = casos.registrar(queja())
    casos.cerrar(folio, "empleado@pecdmx.mx", "Listo.")

    with pytest.raises(CasoNoEncontradoError):
        casos.cerrar(folio, "otro@pecdmx.mx", "Otra vez.")
    with pytest.raises(CasoNoEncontradoError):
        casos.buscar("Q-999999")

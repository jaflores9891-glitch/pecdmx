"""Las rutas de casos para empleados: piden gafete y respetan la sucursal de cada quien."""

from datetime import UTC, datetime

import pytest
from fastapi.testclient import TestClient
from test_gafetes import SECRETO, gafete

from pecdmx_chatbot.app import app
from pecdmx_chatbot.casos import Caso, CasoNoEncontradoError


def caso(folio: str, sucursal: str | None, estado: str = "ABIERTO") -> Caso:
    return Caso(
        folio, "QUEJA", "DANO", 0.9, "PEC0000000018", sucursal, "llegó roto", "ana@correo.mx", estado,
        datetime(2026, 10, 20, tzinfo=UTC), None, None, None,
    )  # fmt: skip


class CasosFalsos:
    def __init__(self) -> None:
        self.guardados = {"Q-000001": caso("Q-000001", "MTY"), "Q-000002": caso("Q-000002", "GDL")}
        self.pedidos: list[tuple] = []

    def listar(self, estado: str = "ABIERTO", sucursal: str | None = None) -> list[Caso]:
        self.pedidos.append((estado, sucursal))
        return list(self.guardados.values())

    def buscar(self, folio: str) -> Caso:
        if folio not in self.guardados:
            raise CasoNoEncontradoError(folio)
        return self.guardados[folio]

    def cerrar(self, folio: str, empleado: str, respuesta: str) -> Caso:
        anterior = self.buscar(folio)
        self.guardados[folio] = Caso(**{**anterior.__dict__, "estado": "CERRADO", "cerrado_por": empleado,
                                        "respuesta": respuesta})  # fmt: skip
        return self.guardados[folio]


@pytest.fixture
def casos():
    app.state.casos = CasosFalsos()
    app.state.jwt_secreto = SECRETO
    yield app.state.casos
    app.state.casos = None
    app.state.jwt_secreto = None


cliente = TestClient(app)


def con_gafete(**datos) -> dict:
    return {"Authorization": f"Bearer {gafete(**datos)}"}


def test_sin_gafete_da_401(casos):
    respuesta = cliente.get("/api/casos")

    assert respuesta.status_code == 401
    assert respuesta.json()["detail"] == "Inicia sesión para ver los casos."


def test_un_gafete_falso_da_401(casos):
    respuesta = cliente.get(
        "/api/casos", headers=con_gafete(secreto="otra-llave-distinta-de-al-menos-32-caracteres")
    )

    assert respuesta.status_code == 401


def test_un_empleado_pide_solo_los_de_su_sucursal(casos):
    respuesta = cliente.get("/api/casos", headers=con_gafete())

    assert respuesta.status_code == 200
    assert casos.pedidos == [("ABIERTO", "MTY")]
    assert respuesta.json()[0]["folio"] == "Q-000001"


def test_un_gerente_pide_todos_y_puede_filtrar_por_estado(casos):
    cliente.get("/api/casos?estado=CERRADO", headers=con_gafete(rol="GERENTE", sucursal=None))

    assert casos.pedidos == [("CERRADO", None)]


def test_un_empleado_cierra_un_caso_de_su_sucursal(casos):
    respuesta = cliente.post(
        "/api/casos/Q-000001/cierre", json={"respuesta": "Se reembolsó."}, headers=con_gafete()
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["estado"] == "CERRADO"
    assert respuesta.json()["cerrado_por"] == "ana@pecdmx.mx"


def test_un_empleado_no_puede_cerrar_casos_de_otra_sucursal(casos):
    respuesta = cliente.post("/api/casos/Q-000002/cierre", json={"respuesta": "Listo."}, headers=con_gafete())

    assert respuesta.status_code == 403
    assert casos.guardados["Q-000002"].estado == "ABIERTO"


def test_cerrar_un_folio_que_no_existe_da_404(casos):
    respuesta = cliente.post("/api/casos/Q-999999/cierre", json={"respuesta": "Listo."}, headers=con_gafete())

    assert respuesta.status_code == 404


def test_sin_llave_configurada_responde_503(casos):
    app.state.jwt_secreto = None

    assert cliente.get("/api/casos", headers=con_gafete()).status_code == 503

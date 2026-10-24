"""Leer los gafetes (JWT) del backend: solo valen los firmados con la llave correcta, vigentes y de PECDMX."""

import time

import jwt
import pytest

from pecdmx_chatbot.gafetes import GafeteInvalidoError, Operador, leer_gafete

SECRETO = "una-llave-de-prueba-de-al-menos-32-caracteres"


def gafete(secreto: str = SECRETO, algoritmo: str = "HS256", **cambios) -> str:
    """Un gafete como los que emite el backend de Java, con lo que se quiera cambiar."""
    ahora = int(time.time())
    datos = {
        "iss": "pecdmx",
        "sub": "ana@pecdmx.mx",
        "iat": ahora,
        "exp": ahora + 3600,
        "nombre": "Ana",
        "rol": "EMPLEADO",
        "sucursal": "MTY",
    }
    datos.update(cambios)
    return jwt.encode({k: v for k, v in datos.items() if v is not None}, secreto, algorithm=algoritmo)


def test_un_gafete_valido_dice_quien_es_y_su_sucursal():
    assert leer_gafete(gafete(), SECRETO) == Operador("ana@pecdmx.mx", "Ana", "EMPLEADO", "MTY")


def test_un_gerente_no_tiene_sucursal_y_ve_todo():
    gerente = leer_gafete(gafete(rol="GERENTE", sucursal=None), SECRETO)

    assert gerente.sucursal is None
    assert gerente.puede_ver("GDL") and gerente.puede_ver(None)


def test_un_empleado_solo_ve_su_sucursal_y_los_casos_sin_sucursal():
    empleado = leer_gafete(gafete(), SECRETO)

    assert empleado.puede_ver("MTY") and empleado.puede_ver(None)
    assert not empleado.puede_ver("GDL")


@pytest.mark.parametrize(
    "token",
    [
        gafete(secreto="otra-llave-distinta-de-al-menos-32-caracteres"),  # firma de alguien más
        gafete(exp=int(time.time()) - 60),  # ya expiró
        gafete(iss="otro-sistema"),  # no lo emitió PECDMX
        gafete(exp=None),  # sin fecha de expiración
        gafete(rol="CLIENTE"),  # no es empleado ni gerente
        gafete(algoritmo="HS512", secreto=SECRETO * 2),  # otro algoritmo
        jwt.encode(
            {"iss": "pecdmx", "sub": "x", "exp": 9999999999, "rol": "GERENTE"}, None, algorithm="none"
        ),
        "esto-no-es-un-gafete",
    ],
)
def test_se_rechazan_los_gafetes_falsos_o_vencidos(token):
    with pytest.raises(GafeteInvalidoError):
        leer_gafete(token, SECRETO)

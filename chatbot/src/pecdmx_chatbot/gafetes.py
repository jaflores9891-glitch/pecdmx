"""Leer el gafete (JWT) que emite el backend de Java, para que los empleados atiendan casos en el chatbot.

El chatbot no tiene usuarios propios: confía en los gafetes firmados con la MISMA llave que el backend
(PECDMX_JWT_SECRETO). Si la firma no coincide, el gafete expiró o no lo emitió "pecdmx", se rechaza.
"""

from dataclasses import dataclass

import jwt

EMISOR = "pecdmx"
ROLES = ("EMPLEADO", "GERENTE")


class GafeteInvalidoError(ValueError):
    pass


@dataclass(frozen=True)
class Operador:
    correo: str
    nombre: str
    rol: str
    # Solo los empleados tienen sucursal; un gerente ve todas.
    sucursal: str | None

    def puede_ver(self, sucursal_del_caso: str | None) -> bool:
        return self.rol == "GERENTE" or sucursal_del_caso is None or sucursal_del_caso == self.sucursal


def leer_gafete(token: str, secreto: str) -> Operador:
    try:
        datos = jwt.decode(
            token,
            secreto.encode("utf-8"),
            algorithms=["HS256"],  # fijo: nunca se acepta el algoritmo que diga el propio gafete
            issuer=EMISOR,
            options={"require": ["exp", "iss", "sub"]},
        )
    except jwt.PyJWTError as error:
        raise GafeteInvalidoError(str(error)) from error
    if datos.get("rol") not in ROLES:
        raise GafeteInvalidoError("El gafete no es de un empleado ni de un gerente.")
    return Operador(datos["sub"], datos.get("nombre", ""), datos["rol"], datos.get("sucursal"))

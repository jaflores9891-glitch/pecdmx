"""Los casos que atiende una persona: quejas y peticiones de hablar con un empleado.

Se guardan en la tabla chatbot.casos (misma base de Neon, esquema del chatbot). Cada caso tiene un folio
para que el cliente le dé seguimiento, y los empleados los ven y cierran con su gafete (JWT).
"""

from dataclasses import dataclass
from datetime import datetime
from typing import Literal

import psycopg
from psycopg.rows import class_row

ESQUEMA = """
CREATE SCHEMA IF NOT EXISTS chatbot;
CREATE TABLE IF NOT EXISTS chatbot.casos (
    id           BIGSERIAL PRIMARY KEY,
    folio        TEXT UNIQUE,
    tipo         TEXT NOT NULL CHECK (tipo IN ('QUEJA', 'ATENCION')),
    categoria    TEXT CHECK (categoria IN ('RETRASO', 'DANO', 'EXTRAVIO', 'OTRO')),
    confianza    REAL,
    guia         TEXT,
    sucursal     TEXT,
    descripcion  TEXT NOT NULL,
    contacto     TEXT NOT NULL,
    estado       TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO')),
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrado_por  TEXT,
    cerrado_en   TIMESTAMPTZ,
    respuesta    TEXT
);
CREATE INDEX IF NOT EXISTS casos_por_estado ON chatbot.casos (estado, sucursal);
"""

Tipo = Literal["QUEJA", "ATENCION"]
PREFIJO = {"QUEJA": "Q", "ATENCION": "A"}


@dataclass(frozen=True)
class CasoNuevo:
    tipo: Tipo
    descripcion: str
    contacto: str
    guia: str | None = None
    # La sucursal de destino de la guía: así cada empleado ve los casos de su sucursal.
    sucursal: str | None = None
    categoria: str | None = None
    confianza: float | None = None


@dataclass(frozen=True)
class Caso:
    folio: str
    tipo: str
    categoria: str | None
    confianza: float | None
    guia: str | None
    sucursal: str | None
    descripcion: str
    contacto: str
    estado: str
    creado_en: datetime
    cerrado_por: str | None
    cerrado_en: datetime | None
    respuesta: str | None


class CasoNoEncontradoError(LookupError):
    pass


class RepositorioDeCasos:
    def __init__(self, url: str) -> None:
        self._url = url

    def crear_esquema(self) -> None:
        with psycopg.connect(self._url, autocommit=True) as conexion:
            conexion.execute(ESQUEMA)

    def registrar(self, caso: CasoNuevo) -> str:
        """Guarda el caso y regresa su folio (ej. Q-000042), hecho con el número consecutivo de la tabla."""
        with psycopg.connect(self._url) as conexion:
            fila = conexion.execute(
                "INSERT INTO chatbot.casos "
                "(tipo, categoria, confianza, guia, sucursal, descripcion, contacto) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s) RETURNING id",
                (
                    caso.tipo,
                    caso.categoria,
                    caso.confianza,
                    caso.guia,
                    caso.sucursal,
                    caso.descripcion,
                    caso.contacto,
                ),
            ).fetchone()
            folio = f"{PREFIJO[caso.tipo]}-{fila[0]:06d}"
            conexion.execute("UPDATE chatbot.casos SET folio = %s WHERE id = %s", (folio, fila[0]))
        return folio

    def listar(self, estado: str = "ABIERTO", sucursal: str | None = None) -> list[Caso]:
        """Los casos en ese estado, del más viejo al más nuevo (primero el que lleva más esperando).

        Con sucursal: solo los de esa sucursal y los que no tienen sucursal (ej. sin número de guía).
        """
        with psycopg.connect(self._url) as conexion, conexion.cursor(row_factory=class_row(Caso)) as cursor:
            cursor.execute(
                f"SELECT {_COLUMNAS} FROM chatbot.casos WHERE estado = %s "  # noqa: S608 (columnas fijas)
                "AND (%s::text IS NULL OR sucursal IS NULL OR sucursal = %s) ORDER BY id",
                (estado, sucursal, sucursal),
            )
            return cursor.fetchall()

    def buscar(self, folio: str) -> Caso:
        with psycopg.connect(self._url) as conexion, conexion.cursor(row_factory=class_row(Caso)) as cursor:
            cursor.execute(f"SELECT {_COLUMNAS} FROM chatbot.casos WHERE folio = %s", (folio,))  # noqa: S608
            caso = cursor.fetchone()
        if caso is None:
            raise CasoNoEncontradoError(folio)
        return caso

    def cerrar(self, folio: str, empleado: str, respuesta: str) -> Caso:
        with psycopg.connect(self._url) as conexion:
            fila = conexion.execute(
                "UPDATE chatbot.casos SET estado = 'CERRADO', cerrado_por = %s, cerrado_en = now(), "
                "respuesta = %s WHERE folio = %s AND estado = 'ABIERTO' RETURNING id",
                (empleado, respuesta, folio),
            ).fetchone()
        if fila is None:
            raise CasoNoEncontradoError(folio)
        return self.buscar(folio)


_COLUMNAS = (
    "folio, tipo, categoria, confianza, guia, sucursal, descripcion, contacto, estado, "
    "creado_en, cerrado_por, cerrado_en, respuesta"
)

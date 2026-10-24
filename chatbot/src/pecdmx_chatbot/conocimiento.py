"""La base de conocimiento del RAG: las políticas con sus embeddings, guardadas en PostgreSQL con pgvector.

Usa su propio esquema ("chatbot"), separado de las tablas del backend de Java, en la misma base de Neon.
"""

from dataclasses import dataclass

import numpy as np
import psycopg
from pgvector.psycopg import register_vector

from pecdmx_chatbot.embeddings import DIMENSIONES, Incrustador
from pecdmx_chatbot.politicas import Fragmento, huella

ESQUEMA = f"""
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS chatbot;
CREATE TABLE IF NOT EXISTS chatbot.fragmentos (
    id        BIGSERIAL PRIMARY KEY,
    archivo   TEXT NOT NULL,
    documento TEXT NOT NULL,
    seccion   TEXT NOT NULL,
    contenido TEXT NOT NULL,
    vector    vector({DIMENSIONES}) NOT NULL
);
-- Datos sueltos del chatbot, como la huella de las políticas ya indexadas.
CREATE TABLE IF NOT EXISTS chatbot.ajustes (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL
);
"""


@dataclass(frozen=True)
class Resultado:
    """Un fragmento encontrado y qué tanto se parece a la pregunta (1 = idéntico)."""

    fragmento: Fragmento
    similitud: float


class BaseDeConocimiento:
    def __init__(self, url: str, incrustador: Incrustador) -> None:
        self._url = url
        self._incrustador = incrustador

    def _conectar(self) -> psycopg.Connection:
        conexion = psycopg.connect(self._url, autocommit=False)
        register_vector(conexion)
        return conexion

    def crear_esquema(self) -> None:
        # La extensión debe existir antes de registrar el tipo "vector", por eso esta conexión no lo registra.
        with psycopg.connect(self._url, autocommit=True) as conexion:
            conexion.execute(ESQUEMA)

    def indexar(self, fragmentos: list[Fragmento]) -> bool:
        """Guarda las políticas con sus embeddings. Si no cambiaron desde la última vez, no hace nada.

        Regresa True si tuvo que (re)indexar. Todo ocurre en una transacción: nunca queda a medias.
        """
        nueva = huella(fragmentos)
        with self._conectar() as conexion:
            fila = conexion.execute(
                "SELECT valor FROM chatbot.ajustes WHERE clave = 'huella_politicas'"
            ).fetchone()
            if fila and fila[0] == nueva:
                return False

            vectores = self._incrustador.incrustar_documentos([f.texto_para_buscar for f in fragmentos])
            conexion.execute("DELETE FROM chatbot.fragmentos")
            with conexion.cursor() as cursor:
                cursor.executemany(
                    "INSERT INTO chatbot.fragmentos (archivo, documento, seccion, contenido, vector) "
                    "VALUES (%s, %s, %s, %s, %s)",
                    [
                        (f.archivo, f.documento, f.seccion, f.contenido, v)
                        for f, v in zip(fragmentos, vectores, strict=True)
                    ],
                )
            conexion.execute(
                "INSERT INTO chatbot.ajustes (clave, valor) VALUES ('huella_politicas', %s) "
                "ON CONFLICT (clave) DO UPDATE SET valor = EXCLUDED.valor",
                (nueva,),
            )
        return True

    def buscar(self, pregunta: str, cuantos: int = 3) -> list[Resultado]:
        """Los fragmentos que más se parecen a la pregunta, del más al menos parecido."""
        vector = self._incrustador.incrustar_consulta(pregunta).astype(np.float32)
        with self._conectar() as conexion:
            filas = conexion.execute(
                # "<=>" es la distancia coseno de pgvector: 0 = mismo significado. Similitud = 1 - distancia.
                "SELECT archivo, documento, seccion, contenido, 1 - (vector <=> %s) AS similitud "
                "FROM chatbot.fragmentos ORDER BY vector <=> %s LIMIT %s",
                (vector, vector, cuantos),
            ).fetchall()
        return [Resultado(Fragmento(*fila[:4]), float(fila[4])) for fila in filas]

    def cantidad_de_fragmentos(self) -> int:
        with self._conectar() as conexion:
            return conexion.execute("SELECT count(*) FROM chatbot.fragmentos").fetchone()[0]

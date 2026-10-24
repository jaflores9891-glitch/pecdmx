"""Piezas compartidas por los tests (pytest busca este archivo solo)."""

from collections.abc import Iterator

import pytest
from testcontainers.community.postgres import PostgresContainer

from pecdmx_chatbot.configuracion import Configuracion
from pecdmx_chatbot.embeddings import Incrustador


@pytest.fixture(scope="session")
def configuracion_de_prueba() -> Configuracion:
    return Configuracion(_env_file=None)


@pytest.fixture(scope="session")
def incrustador_compartido(configuracion_de_prueba: Configuracion) -> Incrustador:
    """El modelo real, cargado una sola vez para todos los tests."""
    return Incrustador(configuracion_de_prueba.modelo_carpeta)


@pytest.fixture(scope="session")
def url_postgres() -> Iterator[str]:
    """Un PostgreSQL 17 con pgvector en Docker, como Neon. Se borra al terminar los tests."""
    with PostgresContainer("pgvector/pgvector:pg17", driver=None) as postgres:
        yield postgres.get_connection_url()

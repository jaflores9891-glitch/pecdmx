"""Clasificador de quejas: retraso, daño, extravío u otro.

Dos piezas:
  1. El modelo de Hugging Face (multilingual-e5-small) convierte la queja en un vector de 384 números.
  2. Una "regresión logística" (lo más sencillo que existe para clasificar) decide el tipo con ese vector.
La pieza 2 la entrenamos nosotros con chatbot/datos/quejas.csv (scripts/entrenar_clasificador.py).
Pesa ~6 KB y se prueba con chatbot/datos/quejas_examen.csv, que nunca se usa para entrenar.
Solo usa numpy: nada de PyTorch en producción.
"""

from dataclasses import dataclass
from pathlib import Path

import numpy as np

from pecdmx_chatbot.embeddings import Incrustador

CATEGORIAS = ("RETRASO", "DANO", "EXTRAVIO", "OTRO")
NOMBRES = {"RETRASO": "Retraso", "DANO": "Daño", "EXTRAVIO": "Extravío", "OTRO": "Otro"}
PESOS = Path(__file__).parent / "clasificador_quejas.npz"
# Si el clasificador está menos seguro que esto, la queja se marca como OTRO para que la revise una persona.
CONFIANZA_MINIMA = 0.5


@dataclass(frozen=True)
class Clasificacion:
    categoria: str
    confianza: float


def softmax(puntajes: np.ndarray) -> np.ndarray:
    """Convierte puntajes en probabilidades que suman 1."""
    exponentes = np.exp(puntajes - puntajes.max(axis=-1, keepdims=True))
    return exponentes / exponentes.sum(axis=-1, keepdims=True)


class ClasificadorDeQuejas:
    def __init__(self, incrustador: Incrustador, pesos: Path = PESOS) -> None:
        self._incrustador = incrustador
        with np.load(pesos) as datos:
            self._w = datos["w"]  # una fila de 384 "importancias" por categoría
            self._b = datos["b"]  # un ajuste por categoría

    def probabilidades(self, texto: str) -> dict[str, float]:
        vector = self._incrustador.incrustar_consulta(texto)
        return dict(zip(CATEGORIAS, softmax(self._w @ vector + self._b).tolist(), strict=True))

    def clasificar(self, texto: str) -> Clasificacion:
        probabilidades = self.probabilidades(texto)
        categoria = max(probabilidades, key=probabilidades.__getitem__)
        confianza = probabilidades[categoria]
        return Clasificacion(categoria if confianza >= CONFIANZA_MINIMA else "OTRO", confianza)

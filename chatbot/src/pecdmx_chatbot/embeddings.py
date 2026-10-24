"""Convierte textos en "embeddings": 384 números que representan su significado.

Textos con significado parecido dan números parecidos, aunque no compartan palabras exactas
("¿a qué hora abren?" se parece a "Horario de atención"). Así funciona la búsqueda del RAG.

Usa intfloat/multilingual-e5-small en ONNX cuantizado: corre con onnxruntime, sin PyTorch.
El tokenizador es SentencePiece (~65 MB de memoria) en lugar de tokenizer.json (~290 MB):
da exactamente los mismos números (ver tests/test_embeddings.py) y cabe en los 512 MB de Render.
"""

from collections.abc import Sequence
from pathlib import Path

import numpy as np
import onnxruntime as ort
import sentencepiece as spm

DIMENSIONES = 384
# El modelo no puede leer más de 512 tokens: 510 piezas de texto + <s> al inicio + </s> al final.
MAXIMO_DE_TOKENS = 512

# Así numera los tokens el modelo (XLM-RoBERTa): los especiales primero y luego los de SentencePiece
# desplazados en 1. SentencePiece usa el 0 para "desconocido", que en el modelo es el 3.
INICIO, FIN, DESCONOCIDO = 0, 2, 3
DESPLAZAMIENTO = 1


class ModeloNoEncontradoError(RuntimeError):
    """Falta el modelo en disco (no se descarga solo, para no hacerlo a escondidas)."""

    def __init__(self, carpeta: Path) -> None:
        super().__init__(
            f"No encontré el modelo de embeddings en {carpeta}. "
            "Descárgalo con: uv run python scripts/descargar_modelo.py"
        )


class Incrustador:
    """Carga el modelo una sola vez y convierte textos en vectores normalizados de 384 números."""

    def __init__(self, carpeta: Path, hilos: int = 1) -> None:
        modelo, tokenizador = carpeta / "modelo.onnx", carpeta / "sentencepiece.bpe.model"
        if not modelo.is_file() or not tokenizador.is_file():
            raise ModeloNoEncontradoError(carpeta)

        # Pocos hilos: en Render gratis hay 0.1 de CPU, y más hilos solo gastarían memoria.
        opciones = ort.SessionOptions()
        opciones.intra_op_num_threads = hilos
        opciones.inter_op_num_threads = 1
        self._sesion = ort.InferenceSession(str(modelo), opciones, providers=["CPUExecutionProvider"])
        self._tokenizador = spm.SentencePieceProcessor(model_file=str(tokenizador))

    # Este modelo se entrenó así: "query: " para preguntas y "passage: " para documentos.
    # Sin esos prefijos, la búsqueda funciona peor.
    def incrustar_consulta(self, texto: str) -> np.ndarray:
        """Una pregunta del cliente → un vector de 384 números."""
        return self._incrustar_uno(f"query: {texto}")

    def incrustar_documentos(self, textos: Sequence[str]) -> np.ndarray:
        """Varios fragmentos de documentos → una matriz de (cantidad, 384)."""
        if not textos:
            return np.empty((0, DIMENSIONES), dtype=np.float32)
        return np.vstack([self._incrustar_uno(f"passage: {texto}") for texto in textos])

    def tokenizar(self, texto: str) -> list[int]:
        """Los números que recibe el modelo: <s>, las piezas del texto (máximo 510) y </s>."""
        # Espacios normalizados (sin repetidos ni en las orillas): así coincide con el tokenizador oficial.
        piezas = self._tokenizador.encode(" ".join(texto.split()))[: MAXIMO_DE_TOKENS - 2]
        return [INICIO, *(DESCONOCIDO if p == 0 else p + DESPLAZAMIENTO for p in piezas), FIN]

    def _incrustar_uno(self, texto: str) -> np.ndarray:
        # Un texto a la vez, a propósito: el modelo cuantizado ajusta su escala mirando todo lo que
        # recibe junto, así que agrupar textos cambiaría (un poco) el resultado de cada uno.
        # Uno por uno, el mismo texto da siempre el mismo vector.
        ids = np.array([self.tokenizar(texto)], dtype=np.int64)
        entradas = {
            "input_ids": ids,
            "attention_mask": np.ones_like(ids),
            "token_type_ids": np.zeros_like(ids),
        }
        (estados,) = self._sesion.run(None, entradas)

        # "Mean pooling": el promedio de los vectores de cada token es el vector del texto.
        promedio = estados[0].mean(axis=0)
        # Normalizar (largo 1) hace que la similitud sea un simple producto punto, entre -1 y 1.
        return promedio / np.linalg.norm(promedio)


def similitud(a: np.ndarray, b: np.ndarray) -> float:
    """Qué tan parecidos son dos vectores normalizados: 1 = mismo significado; más bajo = menos parecido."""
    return float(np.dot(a, b))

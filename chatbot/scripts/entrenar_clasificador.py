"""Entrena el clasificador de quejas con chatbot/datos/quejas.csv y guarda sus pesos.

Uso (desde chatbot/):  uv run python scripts/entrenar_clasificador.py

1. Prueba honesta: entrena con 4 de cada 5 ejemplos y mide el acierto con el quinto que NO vio (5 rondas).
2. Entrena con todos los ejemplos y guarda src/pecdmx_chatbot/clasificador_quejas.npz.
"""

import csv
from pathlib import Path

import numpy as np

from pecdmx_chatbot.clasificador import CATEGORIAS, PESOS, softmax
from pecdmx_chatbot.configuracion import configuracion
from pecdmx_chatbot.embeddings import Incrustador

DATOS = Path(__file__).resolve().parents[1] / "datos" / "quejas.csv"
VUELTAS = 3000
APRENDIZAJE = 0.1
CASTIGO = 0.05  # castiga pesos grandes para que no "memorice" los ejemplos (regularización L2)


def entrenar(x: np.ndarray, y: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Descenso de gradiente: ajusta los pesos poco a poco para equivocarse menos en cada vuelta."""
    # Los vectores de e5 se parecen mucho entre sí. Estandarizar (restar el promedio y dividir entre la
    # dispersión de cada número) resalta lo que distingue a una queja de otra.
    promedio, dispersion = x.mean(axis=0), x.std(axis=0) + 1e-6
    z = (x - promedio) / dispersion

    w = np.zeros((len(CATEGORIAS), x.shape[1]))
    b = np.zeros(len(CATEGORIAS))
    esperado = np.eye(len(CATEGORIAS))[y]
    for _ in range(VUELTAS):
        error = softmax(z @ w.T + b) - esperado
        w -= APRENDIZAJE * (error.T @ z / len(z) + CASTIGO * w)
        b -= APRENDIZAJE * error.mean(axis=0)

    # La estandarización se "mete" dentro de los pesos: así el clasificador solo hace w·x + b.
    w_final = w / dispersion
    return w_final, b - w_final @ promedio


def main() -> None:
    with DATOS.open(encoding="utf-8") as archivo:
        filas = list(csv.DictReader(archivo))
    incrustador = Incrustador(configuracion().modelo_carpeta)
    x = np.stack([incrustador.incrustar_consulta(f["texto"]) for f in filas]).astype(np.float64)
    y = np.array([CATEGORIAS.index(f["categoria"]) for f in filas])
    print(f"{len(filas)} ejemplos: " + ", ".join(f"{c} {np.sum(y == i)}" for i, c in enumerate(CATEGORIAS)))

    aciertos = 0
    for ronda in range(5):
        prueba = np.arange(len(y)) % 5 == ronda
        w, b = entrenar(x[~prueba], y[~prueba])
        predichos = (x[prueba] @ w.T + b).argmax(axis=1)
        aciertos += int(np.sum(predichos == y[prueba]))
        for indice, predicho in zip(np.flatnonzero(prueba), predichos, strict=True):
            if y[indice] != predicho:
                print(f"  falló: «{filas[indice]['texto']}» → {CATEGORIAS[predicho]}")
    print(f"Acierto con ejemplos no vistos: {aciertos / len(y):.1%}")

    w, b = entrenar(x, y)
    np.savez(PESOS, w=w.astype(np.float32), b=b.astype(np.float32))
    print(f"Pesos guardados en {PESOS} ({PESOS.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main()

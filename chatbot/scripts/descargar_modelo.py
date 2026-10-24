"""Descarga el modelo de embeddings (ONNX cuantizado) y su tokenizador, verificando sus huellas SHA-256.

Uso:   uv run python scripts/descargar_modelo.py [carpeta_destino]     (por defecto: chatbot/modelos/)

- Siempre de la MISMA versión del repositorio (un commit fijo), nunca de "la última".
- Si la huella de un archivo no coincide, se borra y el programa falla: protege contra archivos alterados.
- Solo usa lo que ya trae Python, para poder correrlo también al construir la imagen de Docker.
"""

import hashlib
import sys
import tempfile
import urllib.request
from pathlib import Path

REPOSITORIO = "intfloat/multilingual-e5-small"  # autores oficiales, licencia MIT
VERSION = "614241f622f53c4eeff9890bdc4f31cfecc418b3"

# (archivo en el repositorio, nombre local, huella SHA-256 esperada)
ARCHIVOS = (
    (
        "onnx/model_qint8_avx512_vnni.onnx",
        "modelo.onnx",
        "dd476dd0c2514e9b9be83aeb3853fac0763e0bdf4a71645407587d77c48a2d88",
    ),
    # El tokenizador en formato SentencePiece: 5 MB y ~65 MB de memoria
    # (tokenizer.json usaba ~290 MB de memoria, demasiado para los 512 MB de Render).
    (
        "sentencepiece.bpe.model",
        "sentencepiece.bpe.model",
        "cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865",
    ),
)

CARPETA_POR_DEFECTO = Path(__file__).resolve().parent.parent / "modelos"


def huella(ruta: Path) -> str:
    """SHA-256 del archivo, leyéndolo por pedazos para no cargar 118 MB en memoria de golpe."""
    calculadora = hashlib.sha256()
    with ruta.open("rb") as archivo:
        for pedazo in iter(lambda: archivo.read(1024 * 1024), b""):
            calculadora.update(pedazo)
    return calculadora.hexdigest()


def descargar(remoto: str, destino: Path, huella_esperada: str) -> None:
    if destino.exists() and huella(destino) == huella_esperada:
        print(f"✓ {destino.name} ya está descargado y su huella coincide.")
        return

    url = f"https://huggingface.co/{REPOSITORIO}/resolve/{VERSION}/{remoto}"
    print(f"↓ Descargando {remoto} ...")
    # Se descarga a un archivo temporal: si algo falla a la mitad,
    # no queda un archivo a medias con el nombre final.
    with tempfile.NamedTemporaryFile(dir=destino.parent, delete=False) as temporal:
        ruta_temporal = Path(temporal.name)
        with urllib.request.urlopen(url, timeout=120) as respuesta:  # noqa: S310 - URL fija de Hugging Face
            while pedazo := respuesta.read(1024 * 1024):
                temporal.write(pedazo)

    obtenida = huella(ruta_temporal)
    if obtenida != huella_esperada:
        ruta_temporal.unlink()
        raise SystemExit(
            f"✗ La huella de {remoto} NO coincide.\n  esperada: {huella_esperada}\n  obtenida: {obtenida}\n"
            "  El archivo se borró. No se usa un modelo que no sea exactamente el esperado."
        )
    ruta_temporal.replace(destino)
    # Los temporales nacen legibles solo para su dueño; el modelo debe poder leerlo cualquier usuario
    # (en Docker lo descarga root y lo usa el usuario sin permisos "pecdmx"), pero nadie lo modifica.
    destino.chmod(0o644)
    print(f"✓ {destino.name}: {destino.stat().st_size / 1_000_000:.1f} MB, huella verificada.")


def main() -> None:
    carpeta = Path(sys.argv[1]) if len(sys.argv) > 1 else CARPETA_POR_DEFECTO
    carpeta.mkdir(parents=True, exist_ok=True)
    for remoto, local, huella_esperada in ARCHIVOS:
        descargar(remoto, carpeta / local, huella_esperada)
    print(f"Modelo listo en {carpeta}")


if __name__ == "__main__":
    main()

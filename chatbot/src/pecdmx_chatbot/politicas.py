"""Lee las políticas de PECDMX (archivos .md) y las parte en fragmentos citables, uno por sección."""

import hashlib
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Fragmento:
    """Una sección de una política: el pedazo de texto que se busca y se cita."""

    archivo: str
    documento: str
    seccion: str
    contenido: str

    @property
    def cita(self) -> str:
        """Cómo se cita en las respuestas, por ejemplo "Artículos prohibidos y restringidos › Pilas"."""
        return f"{self.documento} › {self.seccion}"

    @property
    def texto_para_buscar(self) -> str:
        """Lo que se convierte en embedding: el título y la sección dan contexto al contenido."""
        return f"{self.documento}. {self.seccion}: {self.contenido}"


def leer_politica(ruta: Path) -> list[Fragmento]:
    """Un archivo .md con un título "# ..." y secciones "## ..." → un fragmento por sección."""
    documento = ""
    fragmentos: list[Fragmento] = []
    seccion: str | None = None
    lineas: list[str] = []

    def cerrar_seccion() -> None:
        contenido = " ".join(" ".join(lineas).split())
        if seccion and contenido:
            fragmentos.append(Fragmento(ruta.name, documento, seccion, contenido))

    for linea in ruta.read_text(encoding="utf-8").splitlines():
        if linea.startswith("## "):
            cerrar_seccion()
            seccion, lineas = linea[3:].strip(), []
        elif linea.startswith("# "):
            documento = linea[2:].strip()
        else:
            lineas.append(linea)
    cerrar_seccion()

    if not documento:
        raise ValueError(f"La política {ruta.name} no tiene título (una línea que empiece con '# ').")
    return fragmentos


def leer_politicas(carpeta: Path) -> list[Fragmento]:
    """Todas las políticas de la carpeta, en orden alfabético de archivo (siempre el mismo orden)."""
    return [f for ruta in sorted(carpeta.glob("*.md")) for f in leer_politica(ruta)]


def huella(fragmentos: list[Fragmento]) -> str:
    """Una "huella digital" del contenido: si cualquier política cambia, la huella cambia."""
    calculadora = hashlib.sha256()
    for f in fragmentos:
        calculadora.update(f"{f.archivo}\x00{f.documento}\x00{f.seccion}\x00{f.contenido}\x00".encode())
    return calculadora.hexdigest()

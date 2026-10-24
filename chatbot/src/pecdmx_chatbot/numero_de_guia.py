"""El número de guía de PECDMX: "PEC" + 9 dígitos + 1 dígito verificador (algoritmo de Luhn).

Es la misma regla que NumeroDeGuia.java en el backend. Aquí sirve para revisar lo que un modelo
"leyó" en una foto: si el dígito verificador no cuadra, la lectura está mal (o la etiqueta).
"""

import re

FORMATO = re.compile(r"PEC\d{10}")
# En una etiqueta el número puede venir con espacios o guiones: "PEC 900000003-5", "PEC 9000 0000 19".
_CANDIDATO = re.compile(r"PEC[\s\-]*((?:\d[\s\-]*){10})(?!\d)", re.IGNORECASE)


def normalizar(texto: str) -> str:
    """Quita espacios y guiones y pasa a mayúsculas: " pec 900000003-5 " → "PEC9000000035"."""
    return re.sub(r"[\s\-]", "", texto).upper()


def digito_verificador(nueve_digitos: str) -> str:
    """Luhn: de derecha a izquierda se duplica un dígito sí y otro no (igual que en el backend)."""
    suma, duplicar = 0, True
    for caracter in reversed(nueve_digitos):
        digito = int(caracter) * (2 if duplicar else 1)
        suma += digito - 9 if digito > 9 else digito
        duplicar = not duplicar
    return str((10 - suma % 10) % 10)


def es_valido(numero: str) -> bool:
    return bool(FORMATO.fullmatch(numero)) and digito_verificador(numero[3:12]) == numero[12]


def candidatos_en(texto: str) -> list[str]:
    """Todos los números con forma de guía dentro de un texto, ya normalizados y sin repetir."""
    return list(dict.fromkeys(normalizar("PEC" + m.group(1)) for m in _CANDIDATO.finditer(texto)))

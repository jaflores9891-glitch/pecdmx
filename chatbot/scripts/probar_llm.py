"""Prueba MANUAL con los modelos reales (gasta un poquito del límite gratis; no es parte de los tests).

Uso, desde chatbot/:   uv run python scripts/probar_llm.py
Lee las llaves del .env y nunca las imprime.
"""

import time

from pecdmx_chatbot.configuracion import configuracion
from pecdmx_chatbot.llm import crear_llm, proveedores_configurados

PREGUNTA = "Responde solo con una frase corta en español: ¿qué es una paquetería?"


def main() -> None:
    config = configuracion()
    for nombre, llave, modelo in (
        ("Groq", config.groq_api_key, config.groq_modelo),
        ("Gemini", config.gemini_api_key, config.gemini_modelo),
    ):
        print(f"{nombre}: {'llave configurada' if llave else 'SIN llave'} · modelo {modelo}")

    for modelo in proveedores_configurados(config):
        nombre = type(modelo).__name__
        inicio = time.perf_counter()
        try:
            # .text da el texto limpio aunque el modelo responda con bloques (como Gemini).
            respuesta = modelo.invoke(PREGUNTA).text
            print(f"\n✅ {nombre} ({time.perf_counter() - inicio:.1f} s): {respuesta}")
        # En una prueba manual se quiere ver cualquier error, del tipo que sea.
        except Exception as error:
            print(f"\n❌ {nombre}: {type(error).__name__}: {error}")

    print("\n== Con respaldo (lo que usará el chatbot)")
    print(crear_llm(config).invoke(PREGUNTA).text)


if __name__ == "__main__":
    main()

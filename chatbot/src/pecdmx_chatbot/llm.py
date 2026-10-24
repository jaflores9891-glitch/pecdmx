"""El "enchufe" único para hablar con un LLM: Groq como principal y Gemini como respaldo.

El resto del chatbot nunca habla con Groq o con Gemini directamente: usa lo que regresa crear_llm().
Ambos son modelos de chat de LangChain, así que se usan igual (esa es la interfaz común).
"""

from collections.abc import Sequence

from langchain_core.language_models import BaseChatModel
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_groq import ChatGroq

from pecdmx_chatbot.configuracion import Configuracion


class SinProveedoresError(RuntimeError):
    """No hay ninguna llave de LLM configurada: el chatbot no puede responder."""

    def __init__(self) -> None:
        super().__init__("No hay ningún LLM configurado. Agrega GROQ_API_KEY o GEMINI_API_KEY al .env.")


def proveedores_configurados(config: Configuracion) -> list[BaseChatModel]:
    """Los modelos disponibles, en orden de preferencia: primero Groq, luego Gemini."""
    proveedores: list[BaseChatModel] = []
    if config.groq_api_key:
        proveedores.append(
            ChatGroq(
                model=config.groq_modelo,
                api_key=config.groq_api_key,
                temperature=config.llm_temperatura,
                timeout=config.llm_espera_segundos,
                # Hasta 3 reintentos: el error más común es el límite por minuto de Groq, que pide esperar
                # milisegundos ("try again in 15ms"). Reintentar es mejor que gastar el cupo diario de Gemini.
                # Para errores largos, el timeout corta la espera y entra el respaldo.
                max_retries=3,
            )
        )
    if config.gemini_api_key:
        proveedores.append(
            ChatGoogleGenerativeAI(
                model=config.gemini_modelo,
                google_api_key=config.gemini_api_key,
                temperature=config.llm_temperatura,
                timeout=config.llm_espera_segundos,
                max_retries=1,
            )
        )
    return proveedores


def encadenar_con_respaldo(
    proveedores: list[BaseChatModel], herramientas: Sequence[BaseTool] = ()
) -> Runnable:
    """Usa el primero; si falla (límite alcanzado, caída, error), intenta con el siguiente, en orden.

    Si hay herramientas, se le enseñan a CADA proveedor antes de encadenarlos: así el respaldo
    también sabe rastrear y cotizar.
    """
    if not proveedores:
        raise SinProveedoresError()
    modelos: list[Runnable] = (
        [p.bind_tools(herramientas) for p in proveedores] if herramientas else proveedores
    )
    principal, *respaldos = modelos
    return principal.with_fallbacks(respaldos) if respaldos else principal


def lectores_de_imagenes(config: Configuracion) -> list[tuple[str, BaseChatModel]]:
    """Modelos con visión, en orden: Qwen en Groq (rápido, más margen gratis) y Gemini de segunda opinión.

    Temperatura 0: para leer un número no se quiere creatividad.
    """
    lectores: list[tuple[str, BaseChatModel]] = []
    if config.groq_api_key:
        lectores.append(
            (
                "groq",
                ChatGroq(
                    model=config.groq_modelo_vision,
                    api_key=config.groq_api_key,
                    temperature=0,
                    timeout=config.llm_espera_segundos,
                    max_retries=0,
                    # Leer un número no necesita "razonar" (era 10 veces más lento) y la respuesta es
                    # corta: pedir poco deja cupo para más fotos dentro del límite por minuto de Groq.
                    reasoning_effort="none",
                    max_tokens=40,
                ),
            )
        )
    if config.gemini_api_key:
        lectores.append(
            (
                "gemini",
                ChatGoogleGenerativeAI(
                    model=config.gemini_modelo_vision,
                    google_api_key=config.gemini_api_key,
                    temperature=0,
                    timeout=config.llm_espera_segundos,
                    max_retries=1,
                ),
            )
        )
    return lectores


def crear_llm(config: Configuracion, herramientas: Sequence[BaseTool] = ()) -> Runnable:
    """El LLM listo para usar, con respaldo automático si hay más de un proveedor."""
    return encadenar_con_respaldo(proveedores_configurados(config), herramientas)

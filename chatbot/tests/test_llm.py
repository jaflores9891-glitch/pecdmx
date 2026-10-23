"""El respaldo entre proveedores, probado con modelos FALSOS: no usa internet ni gasta el límite gratis."""

import pytest
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableWithFallbacks
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_groq import ChatGroq

from pecdmx_chatbot.configuracion import Configuracion
from pecdmx_chatbot.llm import SinProveedoresError, crear_llm, encadenar_con_respaldo


class ModeloQueFalla(FakeListChatModel):
    """Un modelo falso que siempre falla, como Groq cuando llega a su límite (error 429)."""

    responses: list[str] = ["nunca se usa"]

    def _call(self, *args, **kwargs):
        raise RuntimeError("429: límite de peticiones alcanzado")


def falso(respuesta: str) -> FakeListChatModel:
    return FakeListChatModel(responses=[respuesta])


def test_si_el_principal_responde_no_se_usa_el_respaldo():
    llm = encadenar_con_respaldo([falso("respuesta de Groq"), falso("respuesta de Gemini")])

    assert llm.invoke("hola").content == "respuesta de Groq"


def test_si_el_principal_falla_responde_el_respaldo():
    llm = encadenar_con_respaldo([ModeloQueFalla(), falso("respuesta de Gemini")])

    assert llm.invoke("hola").content == "respuesta de Gemini"


def test_si_todos_fallan_se_avisa_el_error():
    llm = encadenar_con_respaldo([ModeloQueFalla(), ModeloQueFalla()])

    with pytest.raises(RuntimeError, match="429"):
        llm.invoke("hola")


def test_text_da_el_texto_limpio_aunque_el_modelo_responda_con_bloques():
    # Así responde Gemini: una lista de bloques, no texto simple. El chatbot siempre debe leer .text.
    respuesta_de_gemini = AIMessage(
        content=[{"type": "text", "text": "Una paquetería entrega paquetes.", "extras": {"signature": "abc"}}]
    )
    respuesta_de_groq = AIMessage(content="Una paquetería entrega paquetes.")

    assert respuesta_de_gemini.text == "Una paquetería entrega paquetes."
    assert respuesta_de_groq.text == "Una paquetería entrega paquetes."


def test_con_un_solo_proveedor_no_hay_respaldo():
    modelo = falso("respuesta")

    assert encadenar_con_respaldo([modelo]) is modelo


def test_sin_proveedores_da_un_error_claro():
    with pytest.raises(SinProveedoresError, match="GROQ_API_KEY o GEMINI_API_KEY"):
        encadenar_con_respaldo([])


@pytest.fixture(autouse=True)
def sin_llaves_reales(monkeypatch):
    """Cada test empieza sin llaves en el entorno, aunque la terminal tuviera alguna cargada."""
    for variable in ("GROQ_API_KEY", "GEMINI_API_KEY", "GROQ_MODELO", "GEMINI_MODELO"):
        monkeypatch.delenv(variable, raising=False)


# Llaves de mentira: crear los modelos no hace ninguna petición a internet.
def config(**valores) -> Configuracion:
    return Configuracion(_env_file=None, **valores)


def test_con_las_dos_llaves_groq_es_el_principal_y_gemini_el_respaldo():
    llm = crear_llm(config(groq_api_key="gsk_falsa", gemini_api_key="falsa", groq_modelo="modelo-groq"))

    assert isinstance(llm, RunnableWithFallbacks)
    assert isinstance(llm.runnable, ChatGroq)
    assert llm.runnable.model_name == "modelo-groq"
    assert [type(r) for r in llm.fallbacks] == [ChatGoogleGenerativeAI]


def test_si_solo_hay_llave_de_gemini_se_usa_gemini_solo():
    assert isinstance(crear_llm(config(gemini_api_key="falsa")), ChatGoogleGenerativeAI)


def test_sin_ninguna_llave_no_se_puede_crear_el_llm():
    with pytest.raises(SinProveedoresError):
        crear_llm(config())

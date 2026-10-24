"""El grafo de LangGraph con un LLM con guion: se revisa el camino pensar → herramienta → pensar."""

from falsos import ConocimientoFalso, LLMConGuion, backend_que_sabe_todo, pide
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage

from pecdmx_chatbot.agente import Asistente, Mensaje
from pecdmx_chatbot.herramientas import crear_herramientas


def asistente_con(guion: list[AIMessage]) -> tuple[Asistente, LLMConGuion]:
    llm = LLMConGuion(guion=guion)
    return Asistente(llm, crear_herramientas(backend_que_sabe_todo(), ConocimientoFalso())), llm


def test_sin_herramientas_responde_directo():
    asistente, llm = asistente_con([AIMessage("¡Hola! ¿En qué te ayudo?")])

    respuesta = asistente.responder([Mensaje("cliente", "hola")])

    assert respuesta.texto == "¡Hola! ¿En qué te ayudo?"
    assert len(llm.recibido) == 1


def test_el_llm_recibe_las_reglas_primero_y_luego_la_conversacion_en_orden():
    asistente, llm = asistente_con([AIMessage("Claro.")])

    asistente.responder(
        [Mensaje("cliente", "hola"), Mensaje("asistente", "¡Hola!"), Mensaje("cliente", "¿y mi paquete?")]
    )

    sistema, *conversacion = llm.recibido[0]
    assert isinstance(sistema, SystemMessage) and "PECDMX" in sistema.text
    assert [type(m) for m in conversacion] == [HumanMessage, AIMessage, HumanMessage]
    assert conversacion[-1].text == "¿y mi paquete?"


def test_para_rastrear_usa_la_herramienta_y_responde_con_su_resultado():
    asistente, llm = asistente_con(
        [pide("rastrear_guia", numero="PEC0000000018"), AIMessage("Tu paquete va en camino a Monterrey.")]
    )

    respuesta = asistente.responder([Mensaje("cliente", "¿dónde está mi guía PEC0000000018?")])

    assert respuesta.texto == "Tu paquete va en camino a Monterrey."
    # En la segunda vuelta el LLM ya tiene el resultado real del backend.
    resultado = llm.recibido[1][-1]
    assert isinstance(resultado, ToolMessage)
    assert "Estado actual: En tránsito" in resultado.text


def test_solo_cuenta_como_fuente_lo_que_de_verdad_se_consulto():
    asistente, _ = asistente_con(
        [
            pide("buscar_politicas", pregunta="¿puedo mandar pilas?"),
            AIMessage(
                "No se envían pilas sueltas. [Fuente: Artículos prohibidos › Pilas] "
                "[Fuente: Política inventada › Nada] [Fuente: Artículos prohibidos › Pilas]"
            ),
        ]
    )

    respuesta = asistente.responder([Mensaje("cliente", "¿puedo mandar pilas?")])

    assert respuesta.fuentes == ["Artículos prohibidos › Pilas"]


def test_si_el_llm_se_cicla_pidiendo_herramientas_se_detiene_con_un_mensaje_amable():
    # El guion repite su último mensaje para siempre: pide herramientas sin parar.
    asistente, llm = asistente_con([pide("rastrear_guia", numero="PEC0000000018")])

    respuesta = asistente.responder([Mensaje("cliente", "¿y mi paquete?")])

    assert "no logré resolver" in respuesta.texto
    assert len(llm.recibido) <= 5


def test_un_argumento_invalido_no_tumba_el_agente_y_el_llm_ve_el_error():
    asistente, llm = asistente_con(
        [
            pide(
                "cotizar_envio", origen="Cancún", destino="GDL", peso_kg=1, largo_cm=1, ancho_cm=1, alto_cm=1
            ),
            AIMessage("No tenemos sucursal en Cancún."),
        ]
    )

    respuesta = asistente.responder([Mensaje("cliente", "¿cuánto cuesta de Cancún a Guadalajara?")])

    assert respuesta.texto == "No tenemos sucursal en Cancún."
    error = llm.recibido[1][-1]
    assert isinstance(error, ToolMessage) and error.status == "error"

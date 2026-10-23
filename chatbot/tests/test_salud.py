from fastapi.testclient import TestClient

from pecdmx_chatbot.app import app

# TestClient hace peticiones a la aplicación sin abrir un servidor real (como MockMvc en Java).
cliente = TestClient(app)


def test_salud_responde_ok():
    respuesta = cliente.get("/salud")

    assert respuesta.status_code == 200
    assert respuesta.json() == {"estado": "ok"}


def test_una_ruta_que_no_existe_da_404():
    assert cliente.get("/no-existe").status_code == 404

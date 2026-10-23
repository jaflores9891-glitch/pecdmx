"""La aplicación web del chatbot: aquí se registran todas sus ventanillas (rutas)."""

from fastapi import FastAPI

app = FastAPI(
    title="PECDMX — Chatbot",
    description="Asistente de servicio al cliente de PECDMX.",
    version="0.1.0",
)


@app.get("/salud")
def salud() -> dict[str, str]:
    """Para saber si el servicio está vivo (Render la usa para revisar que arrancó)."""
    return {"estado": "ok"}

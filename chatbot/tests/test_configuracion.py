from pecdmx_chatbot.configuracion import Configuracion


def test_lee_las_variables_de_entorno(monkeypatch):
    # monkeypatch cambia variables de entorno solo durante este test.
    monkeypatch.setenv("GROQ_API_KEY", "gsk_de_prueba")
    monkeypatch.setenv("GEMINI_MODELO", "gemini-de-prueba")

    config = Configuracion(_env_file=None)

    assert config.groq_api_key.get_secret_value() == "gsk_de_prueba"
    assert config.gemini_modelo == "gemini-de-prueba"


def test_sin_variables_usa_los_modelos_por_defecto(monkeypatch):
    for variable in ("GROQ_API_KEY", "GEMINI_API_KEY", "GROQ_MODELO", "GEMINI_MODELO"):
        monkeypatch.delenv(variable, raising=False)

    config = Configuracion(_env_file=None)

    assert config.groq_api_key is None
    assert config.groq_modelo == "openai/gpt-oss-120b"
    assert config.gemini_modelo == "gemini-3.8-flash"


def test_una_variable_vacia_usa_el_valor_por_defecto(monkeypatch):
    # Como queda al copiar .env.example sin llenar el modelo.
    monkeypatch.setenv("GROQ_MODELO", "")
    monkeypatch.setenv("GROQ_API_KEY", "")

    config = Configuracion(_env_file=None)

    assert config.groq_modelo == "openai/gpt-oss-120b"
    assert config.groq_api_key is None


def test_las_llaves_nunca_se_imprimen(monkeypatch):
    monkeypatch.setenv("GROQ_API_KEY", "gsk_secreta")

    config = Configuracion(_env_file=None)

    assert "gsk_secreta" not in str(config)
    assert "gsk_secreta" not in repr(config)

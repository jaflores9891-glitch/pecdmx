# Notas para la entrevista

Respuestas cortas para explicar el proyecto. Primero la idea en una frase; luego el detalle si te lo piden.

## El pitch (30 segundos)
"PECDMX es el sistema de una paquetería con 6 sucursales. Tiene un backend en Java con arquitectura hexagonal,
una web en React y un asistente con IA en Python que rastrea, cotiza consultando al backend, responde dudas
citando las políticas con RAG, clasifica quejas y pasa casos a un empleado. Está publicado con CI/CD,
casi 500 tests y todo en planes gratuitos."

## Backend (Java 25 + Spring Boot 4)
- **¿Por qué hexagonal?** Las reglas (tarifas, estados, dígito verificador) son Java puro, sin Spring ni base
  de datos. Se prueban en milisegundos y podría cambiar la base o el framework sin tocarlas.
- **¿Cómo evitas estados imposibles?** Cada estado sabe a cuáles puede pasar (`EstadoEnvio.siguientesPosibles`),
  y la base de datos tiene `CHECK` como segunda defensa.
- **¿Dos empleados cambian la misma guía a la vez?** Bloqueo optimista con una columna `version`.
- **Seguridad**: gafetes JWT de 8 horas, contraseñas con BCrypt y permisos por rol y sucursal. El rastreo público
  nunca devuelve datos personales.
- **Tests**: JUnit 5 + Testcontainers con un PostgreSQL 17 real, no una base en memoria que se comporta distinto.

## Chatbot (Python + FastAPI + LangGraph)
- **¿Por qué LangGraph?** El agente es un ciclo explícito `pensar ⇄ herramientas`, con límite de pasos. Lo
  puedo probar con un LLM "con guion" que pide herramientas en un orden conocido.
- **¿Cómo evitas que invente?**
  - Precios y estados siempre salen del backend (herramientas).
  - Las reglas salen de las políticas (RAG).
  - El código descarta las citas que no salieron de la búsqueda.
- **¿Por qué ONNX y no PyTorch?** El plan gratis tiene 512 MB. Con ONNX int8 y el tokenizador SentencePiece,
  el servicio usa ~285 MB; con PyTorch no cabría.
- **El clasificador**: regresión logística en numpy sobre los embeddings de e5, entrenada con 155 quejas.
  Saca 89 % en validación cruzada y 92 % en un examen aparte que nunca vio. Si duda, la marca "otro" para
  revisión humana.
- **¿Y si Groq se cae?** Las herramientas se le enseñan a Groq y a Gemini, y `with_fallbacks` pasa al respaldo solo.
- **Prompt injection**: defensas en capas.
  - Límites de tamaño y de mensajes por minuto.
  - Reglas propias en español.
  - Llama Prompt Guard 2.
  - Instrucciones que tratan al cliente como dato, no como orden.
  - Una palabra trampa en el prompt: si aparece en la respuesta, no se entrega.

## Calidad y operación
- **¿Cómo pruebas algo que no responde siempre igual?** Dos niveles:
  - tests deterministas con un LLM falso, para el código;
  - una evaluación con el LLM real que exige un porcentaje (≥ 85 %) y no el 100 %.
- **Un bug que encontraste en producción**: "¿Puedo mandar un perfume?" a veces decía "no tengo información".
  - Medí con un script que repite la pregunta: 1 de 8 aciertos.
  - El LLM buscaba "…con PECDMX?" y el nombre de la empresa, que aparece en todas las políticas, desviaba la búsqueda.
  - Lo quité de la consulta: 5 de 5 en producción. Agregué el caso a los tests y a la evaluación.
- **CI/CD**:
  - GitHub Actions corre solo lo que cambió.
  - `main` está protegida y los PR se unen con rebase.
  - Render y Vercel publican solos cuando la CI pasa.
- **Secretos**: nunca en el código. Viven en `.env` (ignorado), en los secretos de GitHub y en el panel de cada
  servicio; la CI revisa que no se suba ningún `.env`.

## Lo que mejoraría (siempre te lo preguntan)
- Una pantalla de empleados en la web para atender los casos (hoy es solo por API).
- El límite de mensajes en Redis si hubiera varios servidores.
- Reentrenar el clasificador con quejas reales y medir con más datos.
- Pagar un plan sin arranque en frío, o mantener los servicios despiertos en horario de oficina.
- Búsqueda híbrida (vectores + palabras clave) para un RAG más robusto con muchas más políticas.

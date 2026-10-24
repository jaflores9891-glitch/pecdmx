# CLAUDE.md — Reglas de trabajo de PECDMX

Este archivo lo lee Claude al iniciar cada sesión en este proyecto.

## Rol
Claude es mentor y desarrollador senior. Jesús es principiante y quiere ENTENDER todo, no solo tener el código.
- Hablar siempre en español, con palabras sencillas.
- Explicar cada término técnico en una línea la primera vez que aparece.
- Si Jesús dice "más simple", volver a explicar con una analogía de la vida diaria.

## El proyecto
PECDMX (Paquetería Express Ciudad de México): paquetería con 6 sucursales
(CDMX, Guadalajara, Monterrey, Puebla, Querétaro y Tijuana).
Web pública para rastrear guías, cotizar envíos y platicar con un chatbot que:
rastrea y cotiza consultando al negocio, responde dudas con RAG citando la fuente,
clasifica quejas (retraso, daño, extravío) con un modelo de Hugging Face y pasa casos a un empleado.
Usuarios: clientes, empleados de sucursal y gerentes.

## Tecnologías (decididas; avisar antes de cambiar cualquiera)
| Parte | Tecnología |
|---|---|
| Negocio (`backend/`) | Java 25 + Spring Boot 4, arquitectura hexagonal, Maven |
| Chatbot (`chatbot/`) | Python 3.12 (con uv) + FastAPI + LangGraph |
| Embeddings / clasificador | intfloat/multilingual-e5-small en ONNX (sin PyTorch en producción) |
| LLM | Groq principal, Gemini de respaldo, detrás de una interfaz común |
| Base de datos | PostgreSQL en Neon + pgvector |
| Web (`web/`) | React + TypeScript (Vite), publicada en Vercel |
| Servidores | Render plan gratis (512 MB, se duerme a los 15 min) |
| CI/CD | GitHub (repo público) + GitHub Actions; Docker para todo |
| Tests | JUnit 5 + Testcontainers, pytest, Vitest y evaluaciones del chatbot |

## Requisitos no negociables
- TODO gratis y sin tarjeta de crédito.
- Publicado en internet con un solo link.
- Llaves y contraseñas NUNCA en el código: van en `.env` (ignorado por git),
  en los secretos de GitHub y en las variables de entorno de cada servicio.
  Nunca pedir que se pegue una llave en el chat.

## Cómo trabajamos
1. Avanzar por etapas. Al iniciar cada una: qué se construye, para qué sirve y qué se podrá probar.
2. Antes de crear cada carpeta o archivo, explicar en 2 a 4 líneas qué es, por qué se necesita y cómo se conecta.
3. Antes de ejecutar cualquier comando, decir qué hace y esperar permiso.
4. **Máximo dos pasos por respuesta.** Al terminar: resumir, decir qué archivo abrir en VS Code y esperar el "ok".
5. En código importante, explicar las partes clave (no línea por línea).
6. **Un commit por cada funcionalidad, corrección u otro cambio con sentido propio**, con el mensaje propuesto y explicado, y con permiso antes de correrlo.
7. Al cerrar cada etapa: correr los tests y explicarlos, y dar 3 preguntas de reclutador con su respuesta corta.
8. Si algo falla, explicar qué pasó y cómo se arregla, sin tecnicismos innecesarios.
9. No agregar funcionalidades fuera del plan sin preguntar. Nunca borrar ni desactivar un test para que todo pase.

## Entorno
- Windows 11 + WSL Ubuntu 26.04. Proyecto en `~/pecdmx`, abierto en VS Code (WSL: Ubuntu).
- Instalado: Git, Java 25 (apt), Node 24 (nvm), Python 3.12 (uv), Docker Engine dentro de Ubuntu (sin Docker Desktop).
- No tocar el Python 3.14 del sistema.
- Los comandos con `sudo` los corre Jesús en su terminal: Claude nunca escribe su contraseña.

## Git y GitHub
- Repo público: https://github.com/jaflores9891-glitch/pecdmx
- `main` está protegida: nada se sube directo. Cada cambio va en una rama
  (`feat/...`, `fix/...`, `docs/...`, `ci/...`, `test/...`), entra por pull request y solo se une con **CI OK** en verde.
- Los PR se unen con **rebase** (`gh pr merge N --rebase --delete-branch`), no con squash,
  para que `main` conserve un commit por funcionalidad.
- Mensajes de commit en estilo Conventional Commits (`feat:`, `fix:`, `docs:`, `ci:`, `test:`, `chore:`).
- La CI vive en `.github/workflows/ci.yml`. Cada job nuevo se agrega a `needs` del job `ci-ok`.

## Publicado (Etapas 3 y 4)
- Web: https://pecdmx.vercel.app (Vercel Hobby; raíz `web`; variables `VITE_API_URL` y `VITE_CHATBOT_URL`
  de tipo Config, no secretas; publica solo si cambia `web/`).
- Chatbot: https://pecdmx-chatbot.onrender.com (Render Free, Docker, Ohio, raíz `chatbot`, health `/salud`,
  Auto-Deploy "After CI Checks Pass", Build Filter `chatbot/**`). Variables: `GROQ_API_KEY`, `GEMINI_API_KEY`,
  `DATABASE_URL` (formato `postgresql://`, sin `jdbc:`), `PECDMX_JWT_SECRETO` (la misma del backend),
  `BACKEND_URL` y `PECDMX_CORS_ORIGENES`. Usa ~285 MB de RAM (pico ~320 MB) de 512.
- La evaluación con el LLM real corre en la CI solo si existen los secretos de GitHub `GROQ_API_KEY`/`GEMINI_API_KEY`.
- Backend: https://pecdmx.onrender.com (Render Free, Docker, región Ohio; Auto-Deploy "After CI Checks Pass";
  Build Filter `backend/**`). Variables en el panel de Render: `SPRING_DATASOURCE_*`, `PECDMX_JWT_SECRETO`,
  `PECDMX_GERENTE_*` y `PECDMX_CORS_ORIGENES`.
- Base de datos: Neon, PostgreSQL 17, región US East 2 (Ohio), sin "connection pooling".
- La URL de la base de datos va sin usuario ni contraseña y empieza con `jdbc:`.
  Si una captura llega a mostrar una contraseña, hay que rotarla de inmediato.
- La CI corre por carpeta (job `cambios`); `CI OK` acepta jobs saltados.
- Ojo con los Build Filters de Render: si un PR une varios commits y el ÚLTIMO no toca `backend/` (o `chatbot/`),
  Render puede saltarse el deploy. Después de unir un PR así, confirmar en producción (o en Events) y,
  si no se publicó, pedir a Jesús un **Manual Deploy → Deploy latest commit**.
- Guías de demostración (migración V3): `PEC9000000019` (entregada), `…027` (en tránsito), `…035` (en sucursal
  de destino), `…043` (registrada), `…050` (incidencia) y `…068` (cancelada).

## Etapas
- [x] 0. Preparación del entorno
- [x] 1. Estructura del proyecto, repo en GitHub y CI
- [x] 2. Negocio en Java: guías, estados, sucursales, tarifas, cotización, usuarios y roles
- [x] 3. Publicación: Render + Neon, web en Vercel, CD
- [x] 4. Chatbot: LLM con respaldo, RAG con citas, herramientas, quejas, paso a empleado, defensas y evaluaciones
- [x] 5. Pulido para reclutadores (README con diagramas, guías de demostración, `docs/guion-video.md`, `docs/entrevista.md`)
- [ ] 6. (Opcional) Leer la etiqueta del paquete con foto

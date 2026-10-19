# PECDMX — Paquetería Express Ciudad de México

Proyecto de portafolio: sistema de una paquetería con 6 sucursales
(CDMX, Guadalajara, Monterrey, Puebla, Querétaro y Tijuana).

## Qué puede hacer un cliente
- **Rastrear** un paquete con su número de guía.
- **Cotizar** el precio de un envío entre sucursales.
- **Platicar con un chatbot** que rastrea, cotiza, responde dudas citando las políticas de la empresa,
  levanta quejas clasificadas y pasa el caso a un empleado cuando no puede resolverlo.

## Cómo se conectan las piezas
```
Cliente → Web (React, Vercel) → Negocio (Java, Render) ↔ PostgreSQL + pgvector (Neon)
                              → Chatbot (Python, Render) → Negocio / Base de datos / Groq-Gemini / modelo Hugging Face
```

## Estructura del repositorio
| Carpeta | Qué contiene | Tecnología |
|---|---|---|
| [`backend/`](backend/) | El negocio: guías, sucursales, tarifas, usuarios | Java 25 + Spring Boot 4 |
| [`chatbot/`](chatbot/) | Asistente de servicio al cliente | Python 3.12 + FastAPI + LangGraph |
| [`web/`](web/) | Página pública | React + TypeScript (Vite) |
| [`docs/`](docs/) | Políticas para el RAG y diagramas | Markdown |

## Estado
🚧 En construcción. Etapa actual: **1 — Estructura del proyecto**.

Todo el proyecto usa servicios gratuitos. Las llaves y contraseñas nunca se guardan en el código.

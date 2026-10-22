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

## API del negocio
**En vivo:** https://pecdmx.onrender.com/api/sucursales. Si nadie la usó en 15 minutos, la primera petición tarda en despertarla.

| Petición | Quién | Para qué |
|---|---|---|
| `GET /api/sucursales` | Cualquiera | Lista de sucursales |
| `POST /api/cotizaciones` | Cualquiera | Cotizar un envío (zona, peso cobrable, subtotal, IVA y total) |
| `GET /api/guias/{numero}` | Cualquiera | Rastrear una guía (sin datos personales) |
| `POST /api/sesiones` | Empleados y gerentes | Iniciar sesión y recibir un gafete JWT de 8 horas |
| `POST /api/guias` | Empleado (de su sucursal) o gerente | Registrar una guía |
| `POST /api/guias/{numero}/eventos` | Empleado (en su sucursal) o gerente | Cambiar el estado de una guía |
| `GET /api/guias/{numero}/detalle` | Empleado del origen o destino, o gerente | Ver destinatario y montos |
| `POST /api/usuarios` | Gerente | Dar de alta a un empleado |

Los errores siguen el estándar Problem Details (RFC 9457), con mensajes en español.

## Estado
🚧 En construcción. Etapa actual: **3 — Publicación**.

- [x] 0. Preparación del entorno
- [x] 1. Estructura del proyecto, repositorio y CI
- [x] 2. El negocio en Java
- [ ] 3. Publicación (Render, Neon, Vercel)
- [ ] 4. El chatbot
- [ ] 5. Pulido para reclutadores

Todo el proyecto usa servicios gratuitos. Las llaves y contraseñas nunca se guardan en el código.

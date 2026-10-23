# PECDMX — Paquetería Express Ciudad de México

### 👉 Pruébalo en vivo: **https://pecdmx.vercel.app**

> Servidores gratuitos: si nadie usó la página en 15 minutos, la primera consulta tarda en despertar al backend.
> La página avisa mientras tanto.

Proyecto de portafolio: sistema de una paquetería con 6 sucursales
(CDMX, Guadalajara, Monterrey, Puebla, Querétaro y Tijuana).

## Qué puede hacer un cliente
- **Rastrear** un paquete con su número de guía.
- **Cotizar** el precio de un envío entre sucursales, con su desglose (zona, peso cobrable, IVA y total).
- *(Etapa 4)* **Platicar con un chatbot** que rastrea, cotiza, responde dudas citando las políticas de la empresa,
  levanta quejas clasificadas y pasa el caso a un empleado cuando no puede resolverlo.

## Cómo se conectan las piezas
```
Cliente → Web (React, Vercel) → Negocio (Java, Render) ↔ PostgreSQL (Neon)
                              → Chatbot (Python, Render) → Negocio / Base de datos / Groq-Gemini / modelo Hugging Face
```

## Dónde vive cada parte
| Parte | Servicio (plan gratis) | Link |
|---|---|---|
| Página web | Vercel | https://pecdmx.vercel.app |
| Negocio (API) | Render, imagen de Docker | https://pecdmx.onrender.com/api/sucursales |
| Base de datos | Neon (PostgreSQL 17) | — |

## Cómo se publica (CI/CD)
Nadie publica a mano. `main` está protegida, así que todo cambio entra por pull request:

1. **CI (GitHub Actions)**: un job detecta qué carpetas cambiaron y corre solo esas revisiones:
   tests del backend (JUnit + Testcontainers), construcción de la imagen de Docker y tests, linter y build de la web (Vitest).
   Siempre se revisa que no se haya subido ningún `.env`.
2. El PR solo se puede unir si el semáforo **CI OK** está en verde.
3. **CD**: al llegar a `main`, **Render** publica el backend (solo si cambió `backend/` y la CI pasó) y
   **Vercel** publica la web (solo si cambió `web/`). Cada PR de la web tiene además su propia vista previa en Vercel.

## Estructura del repositorio
| Carpeta | Qué contiene | Tecnología |
|---|---|---|
| [`backend/`](backend/) | El negocio: guías, sucursales, tarifas, usuarios | Java 25 + Spring Boot 4 |
| [`web/`](web/) | Página pública | React + TypeScript (Vite) |
| [`chatbot/`](chatbot/) | Asistente de servicio al cliente (Etapa 4) | Python 3.12 + FastAPI + LangGraph |
| [`docs/`](docs/) | Políticas para el RAG y diagramas | Markdown |

## API del negocio
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
La API solo acepta peticiones del navegador desde la página publicada (CORS).

## Estado
🚧 En construcción. Etapa actual: **4 — El chatbot**.

- [x] 0. Preparación del entorno
- [x] 1. Estructura del proyecto, repositorio y CI
- [x] 2. El negocio en Java
- [x] 3. Publicación (Render, Neon, Vercel) y despliegue automático
- [ ] 4. El chatbot
- [ ] 5. Pulido para reclutadores

Todo el proyecto usa servicios gratuitos, sin tarjeta de crédito. Las llaves y contraseñas nunca se guardan en el código:
viven en las variables de entorno de cada servicio (ver [`.env.example`](.env.example)).

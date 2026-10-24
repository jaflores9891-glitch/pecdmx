# web — Página pública (React)

**En vivo:** https://pecdmx.vercel.app

- **Rastrear**: estado actual e historial de una guía, en hora de la Ciudad de México, con guías de ejemplo a un clic.
- **Cotizar**: precio de un envío entre sucursales, con su desglose.
- **Asistente**: chat con el asistente de IA, con las fuentes de cada respuesta y aviso de privacidad.
- **Aviso de "el servidor está despertando"**: si el backend (Render gratis) tarda más de 3 s, se avisa al cliente.

React 19 + TypeScript + Vite, con estilos en CSS propio. Publicada en Vercel.

## Correrla en tu laptop
```bash
npm ci          # instala exactamente las versiones de package-lock.json
npm run dev     # http://localhost:5173
```
Sin `VITE_API_URL`, la página busca el backend en `http://localhost:8080`.
Para usar el backend publicado, copia [`.env.example`](.env.example) como `.env.local`.

## Revisiones (las mismas que corre la CI)
```bash
npm test        # Vitest + Testing Library, con el backend simulado
npm run lint    # oxlint
npm run build   # TypeScript revisa los tipos y Vite arma la versión final en dist/
```

## Cómo está organizada
| Archivo | Qué hace |
|---|---|
| `src/api.ts` | La única puerta hacia el backend; convierte los errores Problem Details en mensajes en español |
| `src/Rastreo.tsx` | Pestaña Rastrear |
| `src/Cotizador.tsx` | Pestaña Cotizar |
| `src/Chat.tsx` + `src/conversacion.ts` | Pestaña Asistente y qué parte de la conversación se manda al chatbot |
| `src/useServidorDespierto.ts` + `src/AvisoDeDespertar.tsx` | El aviso de servidor despertando |
| `src/tipos.ts` | La forma del JSON del backend |
| `src/fechas.ts`, `src/formatos.ts` | Horas de CDMX, pesos mexicanos y kilos |

## Variable de entorno
| Variable | Para qué |
|---|---|
| `VITE_API_URL` | Dirección pública del backend (ej. `https://pecdmx.onrender.com`). Llega al navegador: nunca debe ser secreta. |
| `VITE_CHATBOT_URL` | Dirección pública del chatbot (ej. `https://pecdmx-chatbot.onrender.com`). Sin ella, `http://localhost:8000`. |

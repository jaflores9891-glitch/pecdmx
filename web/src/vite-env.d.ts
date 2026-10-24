/// <reference types="vite/client" />

// Las variables que Vite inyecta en la página. Solo las que empiezan con VITE_ llegan al navegador,
// así que aquí nunca debe ir nada secreto.
interface ImportMetaEnv {
  /** Dirección pública del backend, por ejemplo https://pecdmx.onrender.com */
  readonly VITE_API_URL?: string
  /** Dirección pública del chatbot, por ejemplo https://pecdmx-chatbot.onrender.com */
  readonly VITE_CHATBOT_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// La única puerta de la página hacia los servidores (backend y chatbot). Todos los componentes pasan por aquí.

/** Dirección del backend. En tu laptop, si no hay VITE_API_URL, se usa el backend local. */
export const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

/** Dirección del chatbot (otro servidor). En tu laptop, si no hay VITE_CHATBOT_URL, se usa el local. */
export const CHATBOT_URL = (import.meta.env.VITE_CHATBOT_URL ?? 'http://localhost:8000').replace(/\/$/, '')

/** Un error que entiende una persona: trae el mensaje en español que mandó el backend (Problem Details). */
export class ErrorDeApi extends Error {
  readonly estado: number

  constructor(estado: number, mensaje: string) {
    super(mensaje)
    this.name = 'ErrorDeApi'
    this.estado = estado
  }
}

/**
 * Hace una petición al backend y regresa su respuesta ya convertida desde JSON.
 * Si el backend responde con error, lanza un ErrorDeApi con su mensaje.
 * Si ni siquiera hay respuesta (sin internet, servidor caído), lanza un ErrorDeApi con estado 0.
 */
export async function pedir<T>(ruta: string, opciones: RequestInit = {}, servidor = API_URL): Promise<T> {
  let respuesta: Response
  try {
    // Con un archivo (FormData) el navegador pone el Content-Type correcto, con su separador; si no, es JSON.
    const esArchivo = opciones.body instanceof FormData
    respuesta = await fetch(`${servidor}${ruta}`, {
      ...opciones,
      headers: esArchivo ? opciones.headers : { 'Content-Type': 'application/json', ...opciones.headers },
    })
  } catch {
    throw new ErrorDeApi(0, 'No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.')
  }

  if (!respuesta.ok) {
    throw new ErrorDeApi(respuesta.status, await mensajeDeError(respuesta))
  }
  return (await respuesta.json()) as T
}

/** Saca el mensaje del error: primero "detail" de Problem Details; si no hay, uno genérico. */
async function mensajeDeError(respuesta: Response): Promise<string> {
  try {
    const problema = (await respuesta.json()) as { detail?: unknown; errores?: string[] }
    if (problema.errores?.length) {
      return problema.errores.join(' ')
    }
    // El chatbot (FastAPI) manda en "detail" un texto o, si la petición no es válida, una lista.
    if (typeof problema.detail === 'string' && problema.detail) {
      return problema.detail
    }
  } catch {
    // La respuesta no era JSON: se usa el mensaje genérico.
  }
  return 'Ocurrió un error inesperado. Intenta de nuevo en unos minutos.'
}

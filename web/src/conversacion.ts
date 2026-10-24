// Las reglas de la conversación con el asistente (sin nada visual): tipos, límites y qué se envía.

export type Rol = 'cliente' | 'asistente'

export interface MensajeDeChat {
  rol: Rol
  texto: string
  fuentes?: string[]
}

export interface RespuestaDelChat {
  respuesta: string
  fuentes: string[]
}

/** Los mismos límites que valida el chatbot: así la página nunca manda algo que él rechazaría. */
export const LARGO_MAXIMO = 1000
const MENSAJES_MAXIMOS = 20
const CARACTERES_MAXIMOS = 6000

/** Si el chatbot tarda más que esto, probablemente estaba dormido: se avisa. */
export const ESPERA_ANTES_DE_AVISAR_MS = 5000

/** Solo los mensajes más recientes que quepan en los límites del chatbot, sin las fuentes. */
export function conversacionParaEnviar(mensajes: MensajeDeChat[]): { rol: Rol; texto: string }[] {
  const recientes: { rol: Rol; texto: string }[] = []
  let caracteres = 0
  for (const { rol, texto } of [...mensajes].reverse()) {
    if (recientes.length === MENSAJES_MAXIMOS || caracteres + texto.length > CARACTERES_MAXIMOS) {
      break
    }
    recientes.unshift({ rol, texto })
    caracteres += texto.length
  }
  return recientes
}

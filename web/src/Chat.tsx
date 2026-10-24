import { useEffect, useRef, useState, type FormEvent } from 'react'
import { CHATBOT_URL, ErrorDeApi, pedir } from './api'
import {
  conversacionParaEnviar,
  ESPERA_ANTES_DE_AVISAR_MS,
  LARGO_MAXIMO,
  type MensajeDeChat,
  type RespuestaDelChat,
} from './conversacion'

const SALUDO: MensajeDeChat = {
  rol: 'asistente',
  texto:
    '¡Hola! Soy el asistente de PECDMX. Puedo rastrear tu guía, cotizar un envío, resolver dudas ' +
    '(qué se puede enviar, horarios, cobros) o levantar una queja. ¿En qué te ayudo?',
}

/** La ventana del asistente: mensajes, aviso de privacidad y la caja para escribir. */
export default function Chat() {
  const [mensajes, setMensajes] = useState<MensajeDeChat[]>([SALUDO])
  const [borrador, setBorrador] = useState('')
  const [esperando, setEsperando] = useState(false)
  const [despertando, setDespertando] = useState(false)
  const [error, setError] = useState('')
  const final = useRef<HTMLDivElement>(null)

  // Cada mensaje nuevo baja la vista hasta el final de la conversación.
  useEffect(() => {
    final.current?.scrollIntoView?.({ behavior: 'smooth', block: 'nearest' })
  }, [mensajes, esperando])

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const texto = borrador.trim()
    if (!texto || esperando) {
      return
    }

    const conMiMensaje: MensajeDeChat[] = [...mensajes, { rol: 'cliente', texto }]
    setMensajes(conMiMensaje)
    setBorrador('')
    setError('')
    setEsperando(true)
    const aviso = setTimeout(() => setDespertando(true), ESPERA_ANTES_DE_AVISAR_MS)

    try {
      const datos = await pedir<RespuestaDelChat>(
        '/api/chat',
        // slice(1): el saludo no se manda, no le aporta nada al chatbot.
        { method: 'POST', body: JSON.stringify({ mensajes: conversacionParaEnviar(conMiMensaje.slice(1)) }) },
        CHATBOT_URL,
      )
      setMensajes([...conMiMensaje, { rol: 'asistente', texto: datos.respuesta, fuentes: datos.fuentes }])
    } catch (e) {
      setError(e instanceof ErrorDeApi ? e.message : 'Ocurrió un error inesperado. Intenta de nuevo.')
      // El mensaje no se pierde: vuelve a la caja para que la persona pueda reintentar.
      setMensajes(mensajes)
      setBorrador(texto)
    } finally {
      clearTimeout(aviso)
      setEsperando(false)
      setDespertando(false)
    }
  }

  return (
    <div className="chat">
      <p className="chat-privacidad">
        🔒 No escribas contraseñas ni datos de tarjetas. Si levantas una queja, guardamos tu correo o teléfono solo
        para responderte. Las respuestas las genera una IA y pueden tener errores.
      </p>

      {/* aria-live: un lector de pantalla lee en voz alta cada mensaje nuevo. */}
      <div className="chat-mensajes" aria-live="polite" aria-label="Conversación">
        {mensajes.map((mensaje, indice) => (
          <div key={indice} className={`burbuja burbuja-${mensaje.rol}`}>
            <span className="solo-lectores">{mensaje.rol === 'cliente' ? 'Tú dijiste:' : 'El asistente dijo:'}</span>
            <p>{mensaje.texto}</p>
            {mensaje.fuentes && mensaje.fuentes.length > 0 && (
              <p className="burbuja-fuentes">Fuente: {mensaje.fuentes.join(' · ')}</p>
            )}
          </div>
        ))}
        {esperando && (
          <div className="burbuja burbuja-asistente burbuja-esperando" role="status">
            <p>{despertando ? 'El asistente está despertando, puede tardar hasta 2 minutos…' : 'Escribiendo…'}</p>
          </div>
        )}
        <div ref={final} />
      </div>

      {error && (
        <p className="alerta" role="alert">
          {error}
        </p>
      )}

      <form className="chat-formulario" onSubmit={enviar}>
        <label htmlFor="mensaje-chat" className="solo-lectores">
          Tu mensaje
        </label>
        <textarea
          id="mensaje-chat"
          value={borrador}
          maxLength={LARGO_MAXIMO}
          rows={2}
          placeholder="Escribe tu pregunta…"
          onChange={(e) => setBorrador(e.target.value)}
          onKeyDown={(e) => {
            // Enter envía; Shift+Enter hace un salto de línea.
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              e.currentTarget.form?.requestSubmit()
            }
          }}
        />
        <button type="submit" className="boton" disabled={esperando || !borrador.trim()}>
          Enviar
        </button>
      </form>
    </div>
  )
}

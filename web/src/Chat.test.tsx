import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Chat from './Chat'
import { conversacionParaEnviar, ESPERA_ANTES_DE_AVISAR_MS, type MensajeDeChat } from './conversacion'

/** El chatbot falso: responde a fetch con el estado y el cuerpo indicados. */
function simularChatbot(estado: number, cuerpo: unknown) {
  const fetchFalso = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } }),
  )
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

function cuerpoEnviado(fetchFalso: ReturnType<typeof vi.fn>, llamada = 0) {
  return JSON.parse(fetchFalso.mock.calls[llamada][1].body as string)
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('Chat', () => {
  it('manda el mensaje, muestra la respuesta y su fuente', async () => {
    const fetchFalso = simularChatbot(200, {
      respuesta: 'No enviamos pilas sueltas.',
      fuentes: ['Artículos prohibidos y restringidos › Pilas, baterías y power banks'],
    })
    const persona = userEvent.setup()
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), '¿puedo mandar una pila?')
    await persona.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(await screen.findByText('No enviamos pilas sueltas.')).toBeInTheDocument()
    expect(screen.getByText(/Fuente: Artículos prohibidos/)).toBeInTheDocument()
    expect(fetchFalso.mock.calls[0][0]).toMatch(/\/api\/chat$/)
    // El saludo no se manda: solo lo que escribió la persona.
    expect(cuerpoEnviado(fetchFalso)).toEqual({ mensajes: [{ rol: 'cliente', texto: '¿puedo mandar una pila?' }] })
    expect(screen.getByLabelText('Tu mensaje')).toHaveValue('')
  })

  it('en el segundo mensaje manda la conversación completa', async () => {
    const fetchFalso = simularChatbot(200, { respuesta: '¿Cuánto pesa?', fuentes: [] })
    const persona = userEvent.setup()
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), 'quiero cotizar{Enter}')
    await screen.findByText('¿Cuánto pesa?')
    await persona.type(screen.getByLabelText('Tu mensaje'), '3 kg{Enter}')
    await vi.waitFor(() => expect(fetchFalso).toHaveBeenCalledTimes(2))

    expect(cuerpoEnviado(fetchFalso, 1).mensajes).toEqual([
      { rol: 'cliente', texto: 'quiero cotizar' },
      { rol: 'asistente', texto: '¿Cuánto pesa?' },
      { rol: 'cliente', texto: '3 kg' },
    ])
  })

  it('no manda mensajes vacíos', async () => {
    const fetchFalso = simularChatbot(200, { respuesta: 'x', fuentes: [] })
    const persona = userEvent.setup()
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), '   ')

    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled()
    expect(fetchFalso).not.toHaveBeenCalled()
  })

  it('si el chatbot falla, muestra su mensaje y devuelve el texto a la caja para reintentar', async () => {
    simularChatbot(429, { detail: 'Vas muy rápido. Espera un minuto y vuelve a intentarlo.' })
    const persona = userEvent.setup()
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), 'hola{Enter}')

    expect(await screen.findByRole('alert')).toHaveTextContent('Vas muy rápido')
    expect(screen.getByLabelText('Tu mensaje')).toHaveValue('hola')
    const conversacion = screen.getByLabelText('Conversación')
    expect(within(conversacion).queryByText('hola')).not.toBeInTheDocument()
  })

  it('si la petición no es válida (422 de FastAPI), muestra un mensaje entendible', async () => {
    simularChatbot(422, { detail: [{ type: 'string_too_long', loc: ['body'], msg: 'muy largo' }] })
    const persona = userEvent.setup()
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), 'hola{Enter}')

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado')
  })

  it('si tarda más de 5 segundos, avisa que el asistente está despertando', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
    const persona = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    render(<Chat />)

    await persona.type(screen.getByLabelText('Tu mensaje'), 'hola{Enter}')
    expect(screen.getByRole('status')).toHaveTextContent('Escribiendo')
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_ANTES_DE_AVISAR_MS + 100)
    })

    expect(screen.getByRole('status')).toHaveTextContent('está despertando')
  })

  it('muestra el aviso de privacidad', () => {
    render(<Chat />)

    expect(screen.getByText(/No escribas contraseñas ni datos de tarjetas/)).toBeInTheDocument()
  })
})

describe('conversacionParaEnviar', () => {
  it('manda como máximo los 20 mensajes más recientes', () => {
    const mensajes: MensajeDeChat[] = Array.from({ length: 25 }, (_, i) => ({ rol: 'cliente', texto: `m${i}` }))

    const enviados = conversacionParaEnviar(mensajes)

    expect(enviados).toHaveLength(20)
    expect(enviados[0].texto).toBe('m5')
    expect(enviados.at(-1)?.texto).toBe('m24')
  })

  it('deja fuera los mensajes viejos si se pasan de 6000 caracteres en total', () => {
    const mensajes: MensajeDeChat[] = Array.from({ length: 8 }, (_, i) => ({ rol: 'cliente', texto: `${i}`.repeat(900) }))

    const enviados = conversacionParaEnviar(mensajes)

    expect(enviados).toHaveLength(6)
    expect(enviados.at(-1)?.texto.startsWith('7')).toBe(true)
  })
})

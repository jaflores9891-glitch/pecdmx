import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AvisoDeDespertar from './AvisoDeDespertar'
import { ESPERA_ANTES_DE_AVISAR_MS } from './useServidorDespierto'

/** Un servidor falso que contesta solo cuando el test lo decide. */
function servidorQueTarda() {
  let contestar: () => void = () => {}
  const respuesta = new Promise<Response>((resolver) => {
    contestar = () => resolver(new Response('[]', { status: 200 }))
  })
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(respuesta))
  return { contestar }
}

beforeEach(() => {
  // Relojes falsos: el test adelanta el tiempo al instante, sin esperar segundos de verdad.
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('AvisoDeDespertar', () => {
  it('si el servidor contesta rápido, nunca muestra el aviso', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('[]', { status: 200 })))
    render(<AvisoDeDespertar />)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_ANTES_DE_AVISAR_MS + 1000)
    })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('antes de 3 segundos todavía no avisa', async () => {
    servidorQueTarda()
    render(<AvisoDeDespertar />)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_ANTES_DE_AVISAR_MS - 100)
    })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('si tarda más de 3 segundos, avisa que está despertando, y el aviso se va cuando contesta', async () => {
    const servidor = servidorQueTarda()
    render(<AvisoDeDespertar />)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_ANTES_DE_AVISAR_MS + 100)
    })
    expect(screen.getByRole('status')).toHaveTextContent('El servidor está despertando')
    expect(screen.getByRole('status')).toHaveTextContent('hasta 2 minutos')

    // Pasan 90 segundos más y el servidor por fin contesta.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(90_000)
      servidor.contestar()
    })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('si no hay conexión, lo avisa con un mensaje claro', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    render(<AvisoDeDespertar />)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })

    expect(screen.getByRole('alert')).toHaveTextContent('No pudimos conectar con el servidor')
  })

  it('pregunta a /api/sucursales (dentro de las reglas de CORS), no a /actuator/health', () => {
    servidorQueTarda()
    render(<AvisoDeDespertar />)

    expect(vi.mocked(fetch).mock.calls[0][0]).toMatch(/\/api\/sucursales$/)
  })
})

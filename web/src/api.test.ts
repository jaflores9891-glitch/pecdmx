import { afterEach, describe, expect, it, vi } from 'vitest'
import { API_URL, ErrorDeApi, pedir } from './api'

/** Reemplaza fetch (la función del navegador que hace peticiones) por una respuesta inventada. */
function simularRespuesta(estado: number, cuerpo: unknown) {
  const fetchFalso = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } }),
  )
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('pedir', () => {
  it('regresa el JSON del backend cuando todo sale bien', async () => {
    const fetchFalso = simularRespuesta(200, [{ codigo: 'MTY', ciudad: 'Monterrey' }])

    const sucursales = await pedir<{ codigo: string }[]>('/api/sucursales')

    expect(sucursales[0].codigo).toBe('MTY')
    expect(fetchFalso).toHaveBeenCalledWith(`${API_URL}/api/sucursales`, expect.anything())
  })

  it('con un error del backend usa su mensaje en español (Problem Details)', async () => {
    simularRespuesta(404, { status: 404, title: 'Guía no encontrada', detail: 'No encontramos la guía PEC0000000018.' })

    await expect(pedir('/api/guias/PEC0000000018')).rejects.toMatchObject({
      estado: 404,
      message: 'No encontramos la guía PEC0000000018.',
    })
  })

  it('si faltan datos, junta la lista de errores del backend', async () => {
    simularRespuesta(400, { detail: 'Faltan datos', errores: ['El destino es obligatorio', 'El peso es obligatorio'] })

    await expect(pedir('/api/cotizaciones', { method: 'POST' })).rejects.toThrow(
      'El destino es obligatorio El peso es obligatorio',
    )
  })

  it('si no hay conexión, da un mensaje claro con estado 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await pedir('/api/sucursales').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorDeApi)
    expect(error).toMatchObject({ estado: 0, message: expect.stringContaining('No pudimos conectar') })
  })

  it('si el error no trae JSON, usa un mensaje genérico', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Bad Gateway', { status: 502 })))

    await expect(pedir('/api/sucursales')).rejects.toThrow('Ocurrió un error inesperado')
  })
})

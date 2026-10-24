import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Rastreo from './Rastreo'
import type { Rastreo as DatosDeRastreo } from './tipos'

const CDMX = { codigo: 'CDMX', ciudad: 'Ciudad de México', estado: 'Ciudad de México' }
const MTY = { codigo: 'MTY', ciudad: 'Monterrey', estado: 'Nuevo León' }
const REGISTRADA = { codigo: 'REGISTRADA', nombre: 'Registrada', descripcion: 'Recibimos tu paquete.' }
const EN_TRANSITO = { codigo: 'EN_TRANSITO', nombre: 'En tránsito', descripcion: 'Tu paquete va en camino.' }

// Una respuesta como la que manda el backend real.
const RASTREO: DatosDeRastreo = {
  numero: 'PEC2610190049',
  origen: CDMX,
  destino: MTY,
  estado: EN_TRANSITO,
  historial: [
    { estado: REGISTRADA, fechaHora: '2026-10-20T15:00:00Z', sucursal: CDMX, nota: 'Paquete recibido en sucursal' },
    { estado: EN_TRANSITO, fechaHora: '2026-10-20T17:30:00Z', sucursal: CDMX, nota: 'Salió en ruta a Monterrey' },
  ],
}

/** El backend falso: responde a fetch con el estado y el cuerpo indicados. */
function simularBackend(estado: number, cuerpo: unknown) {
  const fetchFalso = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } }),
  )
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

async function buscarGuia(numero: string) {
  const persona = userEvent.setup()
  render(<Rastreo />)
  await persona.type(screen.getByLabelText('Número de guía'), numero)
  await persona.click(screen.getByRole('button', { name: 'Buscar' }))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Rastreo', () => {
  it('muestra el estado actual, la ruta y el historial con lo más reciente primero', async () => {
    const fetchFalso = simularBackend(200, RASTREO)

    await buscarGuia(' pec2610190049 ')

    expect(await screen.findByRole('heading', { name: 'En tránsito' })).toBeInTheDocument()
    expect(screen.getByText('Tu paquete va en camino.')).toBeInTheDocument()
    expect(screen.getByText('Ciudad de México → Monterrey')).toBeInTheDocument()

    const eventos = within(screen.getByRole('list')).getAllByRole('listitem')
    expect(eventos).toHaveLength(2)
    expect(eventos[0]).toHaveTextContent('En tránsito')
    expect(eventos[0]).toHaveTextContent('Salió en ruta a Monterrey')
    expect(eventos[1]).toHaveTextContent('Registrada')

    // Se manda el número sin espacios.
    expect(fetchFalso.mock.calls[0][0]).toMatch(/\/api\/guias\/pec2610190049$/)
  })

  it('muestra la hora en horario de la Ciudad de México, no en UTC', async () => {
    simularBackend(200, RASTREO)

    await buscarGuia('PEC2610190049')

    // 15:00 UTC son las 9:00 en la Ciudad de México.
    const registrada = (await screen.findAllByRole('listitem'))[1]
    expect(registrada).toHaveTextContent(/20 oct 2026/)
    expect(registrada).toHaveTextContent(/9:00/)
  })

  it('si la guía no existe, muestra el mensaje del backend', async () => {
    simularBackend(404, { status: 404, detail: 'No encontramos la guía PEC0000000018. Revisa el número.' })

    await buscarGuia('PEC0000000018')

    expect(await screen.findByRole('alert')).toHaveTextContent('No encontramos la guía PEC0000000018')
    expect(screen.queryByRole('heading', { level: 2 })).not.toBeInTheDocument()
  })

  it('si la guía está mal escrita, muestra el mensaje del backend', async () => {
    simularBackend(400, { status: 400, detail: 'El número de guía no es válido. Revisa que esté bien escrito.' })

    await buscarGuia('PEC2610190042')

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa que esté bien escrito')
  })

  it('mientras busca, el botón dice "Buscando…" y está desactivado', async () => {
    // Una respuesta que nunca llega, para ver el estado "buscando".
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

    await buscarGuia('PEC2610190049')

    expect(screen.getByRole('button', { name: 'Buscando…' })).toBeDisabled()
  })

  it('con la casilla vacía no se puede buscar', async () => {
    const fetchFalso = simularBackend(200, RASTREO)
    const persona = userEvent.setup()
    render(<Rastreo />)

    await persona.type(screen.getByLabelText('Número de guía'), '   ')

    expect(screen.getByRole('button', { name: 'Buscar' })).toBeDisabled()
    expect(fetchFalso).not.toHaveBeenCalled()
  })

  it('un clic en una guía de ejemplo la escribe y la busca', async () => {
    const fetchFalso = simularBackend(200, RASTREO)
    const persona = userEvent.setup()
    render(<Rastreo />)

    await persona.click(screen.getByRole('button', { name: 'en tránsito' }))

    expect(await screen.findByRole('heading', { name: 'En tránsito' })).toBeInTheDocument()
    expect(screen.getByLabelText('Número de guía')).toHaveValue('PEC9000000027')
    expect(fetchFalso.mock.calls[0][0]).toMatch(/\/api\/guias\/PEC9000000027$/)
  })
})

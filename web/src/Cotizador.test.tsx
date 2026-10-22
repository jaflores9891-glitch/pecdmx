import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Cotizador from './Cotizador'
import type { Cotizacion } from './tipos'
import { pesos } from './formatos'

const CDMX = { codigo: 'CDMX', ciudad: 'Ciudad de México', estado: 'Ciudad de México' }
const MTY = { codigo: 'MTY', ciudad: 'Monterrey', estado: 'Nuevo León' }

// El ejemplo del negocio, tal como lo responde el backend real.
const COTIZACION: Cotizacion = {
  origen: CDMX,
  destino: MTY,
  zona: 3,
  descripcionZona: 'Media (400 a 1,000 km)',
  pesoRealKg: 3.2,
  pesoVolumetricoKg: 4.8,
  pesoCobrableKg: 5,
  subtotal: 310,
  iva: 49.6,
  total: 359.6,
  moneda: 'MXN',
}

function respuesta(estado: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } })
}

/** Backend falso: GET /api/sucursales da la lista; POST /api/cotizaciones da lo que se indique. */
function simularBackend(cotizar: () => Response) {
  const fetchFalso = vi.fn((url: string, _opciones?: RequestInit) =>
    Promise.resolve(url.endsWith('/api/sucursales') ? respuesta(200, [CDMX, MTY]) : cotizar()),
  )
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

async function llenarYCotizar(peso = '3.2') {
  const persona = userEvent.setup()
  render(<Cotizador />)
  // Esperar a que lleguen las sucursales para poder elegirlas.
  await within(screen.getByLabelText('Destino')).findByRole('option', { name: 'Monterrey' })
  await persona.selectOptions(screen.getByLabelText('Origen'), 'CDMX')
  await persona.selectOptions(screen.getByLabelText('Destino'), 'MTY')
  await persona.type(screen.getByLabelText('Peso (kg)'), peso)
  await persona.type(screen.getByLabelText('Largo (cm)'), '40')
  await persona.type(screen.getByLabelText('Ancho (cm)'), '30')
  await persona.type(screen.getByLabelText('Alto (cm)'), '20')
  await persona.click(screen.getByRole('button', { name: 'Cotizar' }))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Cotizador', () => {
  it('mientras llegan las sucursales, los menús dicen "Cargando sucursales…"', () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
    render(<Cotizador />)

    expect(screen.getByLabelText('Origen')).toBeDisabled()
    expect(within(screen.getByLabelText('Origen')).getByRole('option')).toHaveTextContent('Cargando sucursales…')
  })

  it('cotiza el ejemplo del negocio y muestra el desglose completo', async () => {
    const fetchFalso = simularBackend(() => respuesta(200, COTIZACION))

    await llenarYCotizar()

    const resultado = await screen.findByRole('region', { name: 'Resultado de la cotización' })
    expect(resultado).toHaveTextContent('Zona 3 · Media (400 a 1,000 km)')
    expect(resultado).toHaveTextContent('Ciudad de México → Monterrey')
    expect(resultado).toHaveTextContent('Se cobran 5 kg')
    expect(resultado).toHaveTextContent('$310.00')
    expect(resultado).toHaveTextContent('$49.60')
    expect(resultado).toHaveTextContent('$359.60 MXN')

    // Se mandan números (no texto) al backend.
    const [, opciones] = fetchFalso.mock.calls.find(([url]) => url.endsWith('/api/cotizaciones'))!
    expect(JSON.parse(opciones?.body as string)).toEqual({
      origen: 'CDMX',
      destino: 'MTY',
      pesoKg: 3.2,
      largoCm: 40,
      anchoCm: 30,
      altoCm: 20,
    })
  })

  it('si el paquete pesa más de 70 kg, muestra el mensaje del backend', async () => {
    simularBackend(() => respuesta(400, { status: 400, detail: 'El peso máximo por paquete es de 70 kg.' }))

    await llenarYCotizar('80')

    expect(await screen.findByRole('alert')).toHaveTextContent('El peso máximo por paquete es de 70 kg.')
  })

  it('no se puede cotizar hasta llenar todo', async () => {
    simularBackend(() => respuesta(200, COTIZACION))
    const persona = userEvent.setup()
    render(<Cotizador />)
    await waitFor(() => expect(screen.getByLabelText('Origen')).toBeEnabled())

    await persona.selectOptions(screen.getByLabelText('Origen'), 'CDMX')

    expect(screen.getByRole('button', { name: 'Cotizar' })).toBeDisabled()
  })

  it('si no se pueden cargar las sucursales, lo avisa', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    render(<Cotizador />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos conectar con el servidor')
  })
})

describe('pesos', () => {
  it('da formato de pesos mexicanos con separador de miles', () => {
    expect(pesos(2705)).toBe('$2,705.00')
    expect(pesos(49.6)).toBe('$49.60')
  })
})

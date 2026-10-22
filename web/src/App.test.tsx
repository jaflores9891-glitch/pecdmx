import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

// La pestaña Cotizar pide las sucursales al abrirse: un backend falso evita que el test dependa de internet.
beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response('[]', { status: 200 }))))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('App', () => {
  it('muestra el nombre de la empresa', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /PECDMX/ })).toBeInTheDocument()
    expect(screen.getByText('Paquetería Express Ciudad de México')).toBeInTheDocument()
  })

  it('tiene las pestañas Rastrear y Cotizar, y abre en Rastrear', () => {
    render(<App />)

    expect(screen.getByRole('tab', { name: 'Rastrear' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Cotizar' })).toHaveAttribute('aria-selected', 'false')
    expect(screen.getByRole('tabpanel')).toHaveTextContent('número de guía')
  })

  it('al hacer clic en Cotizar cambia la pestaña y su contenido', async () => {
    const persona = userEvent.setup()
    render(<App />)

    await persona.click(screen.getByRole('tab', { name: 'Cotizar' }))

    expect(screen.getByRole('tab', { name: 'Cotizar' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Rastrear' })).toHaveAttribute('aria-selected', 'false')
    expect(screen.getByRole('tabpanel')).toHaveTextContent('cuánto cuesta enviar')
  })

  it('enlaza al código en GitHub', () => {
    render(<App />)

    expect(screen.getByRole('link', { name: 'Código en GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/jaflores9891-glitch/pecdmx',
    )
  })
})

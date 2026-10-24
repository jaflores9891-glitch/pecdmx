import { useEffect, useState } from 'react'
import { CHATBOT_URL } from './api'
import AvisoDeDespertar from './AvisoDeDespertar'
import Chat from './Chat'
import Cotizador from './Cotizador'
import Rastreo from './Rastreo'

type Pestana = 'rastrear' | 'cotizar' | 'asistente'

const PESTANAS: { id: Pestana; titulo: string }[] = [
  { id: 'rastrear', titulo: 'Rastrear' },
  { id: 'cotizar', titulo: 'Cotizar' },
  { id: 'asistente', titulo: 'Asistente' },
]

const PANELES: Record<Pestana, () => React.JSX.Element> = {
  rastrear: Rastreo,
  cotizar: Cotizador,
  asistente: Chat,
}

/** La página completa: encabezado, pestañas y pie. Cada pestaña mostrará su propio componente. */
export default function App() {
  // "useState" guarda qué pestaña está abierta; al cambiarla, React vuelve a pintar la página.
  const [pestana, setPestana] = useState<Pestana>('rastrear')
  const Panel = PANELES[pestana]

  // "Toca la puerta" del chatbot al abrir la página: si estaba dormido, empieza a despertar ya,
  // antes de que alguien le escriba. No importa la respuesta (ni si falla).
  useEffect(() => {
    fetch(`${CHATBOT_URL}/salud`).catch(() => {})
  }, [])

  return (
    <div className="pagina">
      <header className="encabezado">
        <h1>
          <span aria-hidden="true">📦</span> PECDMX
        </h1>
        <p>Paquetería Express Ciudad de México</p>
      </header>

      <main className="contenido">
        <AvisoDeDespertar />

        <div className="pestanas" role="tablist" aria-label="Servicios">
          {PESTANAS.map(({ id, titulo }) => (
            <button
              key={id}
              type="button"
              role="tab"
              id={`pestana-${id}`}
              aria-selected={pestana === id}
              aria-controls={`panel-${id}`}
              className="pestana"
              onClick={() => setPestana(id)}
            >
              {titulo}
            </button>
          ))}
        </div>

        <section role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`pestana-${pestana}`} className="panel">
          <Panel />
        </section>
      </main>

      <footer className="pie">
        <a href="https://github.com/jaflores9891-glitch/pecdmx">Código en GitHub</a>
        <span> · Proyecto de portafolio de Jesús Flores</span>
      </footer>
    </div>
  )
}

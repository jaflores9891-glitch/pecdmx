import { useState, type FormEvent } from 'react'
import { ErrorDeApi, pedir } from './api'
import { fechaHoraLegible } from './fechas'
import type { Rastreo as DatosDeRastreo } from './tipos'

/** Guías de demostración que existen en la base (migración V3 del backend), para quien visita la página. */
const GUIAS_DE_EJEMPLO = [
  { numero: 'PEC9000000027', estado: 'en tránsito' },
  { numero: 'PEC9000000035', estado: 'lista para recoger' },
  { numero: 'PEC9000000050', estado: 'con incidencia' },
  { numero: 'PEC9000000019', estado: 'entregada' },
]

/** Pestaña "Rastrear": el cliente escribe su número de guía y ve el estado y el historial de su paquete. */
export default function Rastreo() {
  const [numero, setNumero] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [rastreo, setRastreo] = useState<DatosDeRastreo | null>(null)
  const [error, setError] = useState<string | null>(null)

  const numeroLimpio = numero.trim()

  async function consultar(guia: string) {
    if (!guia || buscando) {
      return
    }
    setBuscando(true)
    setError(null)
    setRastreo(null)
    try {
      // encodeURIComponent protege la URL si el cliente escribe espacios o símbolos.
      setRastreo(await pedir<DatosDeRastreo>(`/api/guias/${encodeURIComponent(guia)}`))
    } catch (e) {
      setError(e instanceof ErrorDeApi ? e.message : 'Ocurrió un error inesperado. Intenta de nuevo.')
    } finally {
      setBuscando(false)
    }
  }

  function buscar(evento: FormEvent<HTMLFormElement>) {
    // Sin esto, el navegador recargaría la página al enviar el formulario.
    evento.preventDefault()
    void consultar(numeroLimpio)
  }

  function probarEjemplo(guia: string) {
    setNumero(guia)
    void consultar(guia)
  }

  return (
    <div>
      <p>Escribe tu número de guía para ver dónde va tu paquete.</p>

      <form className="formulario-en-linea" onSubmit={buscar}>
        <label htmlFor="numero-de-guia">Número de guía</label>
        <div className="fila">
          <input
            id="numero-de-guia"
            value={numero}
            onChange={(e) => setNumero(e.target.value)}
            placeholder="PEC9000000027"
            autoComplete="off"
            spellCheck={false}
          />
          <button type="submit" className="boton" disabled={!numeroLimpio || buscando}>
            {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>

      <p className="ejemplos">
        ¿No tienes una guía? Prueba con:{' '}
        {GUIAS_DE_EJEMPLO.map(({ numero: guia, estado }) => (
          <button
            key={guia}
            type="button"
            className="ejemplo"
            disabled={buscando}
            onClick={() => probarEjemplo(guia)}
          >
            {estado}
          </button>
        ))}
      </p>

      {/* role="alert" hace que un lector de pantalla lea el error en voz alta en cuanto aparece. */}
      {error && (
        <p className="alerta" role="alert">
          {error}
        </p>
      )}

      {rastreo && <ResultadoDeRastreo rastreo={rastreo} />}
    </div>
  )
}

function ResultadoDeRastreo({ rastreo }: { rastreo: DatosDeRastreo }) {
  // Lo más reciente primero: es lo que el cliente quiere ver.
  const historial = [...rastreo.historial].reverse()

  return (
    <section className="resultado" aria-label={`Rastreo de la guía ${rastreo.numero}`}>
      <div className="estado-actual">
        <p className="etiqueta">Guía {rastreo.numero}</p>
        <h2>{rastreo.estado.nombre}</h2>
        <p>{rastreo.estado.descripcion}</p>
        <p className="ruta">
          {rastreo.origen.ciudad} → {rastreo.destino.ciudad}
        </p>
      </div>

      <h3>Historial</h3>
      <ol className="linea-de-tiempo">
        {historial.map((evento) => (
          <li key={`${evento.fechaHora}-${evento.estado.codigo}`}>
            <p className="evento-titulo">
              <strong>{evento.estado.nombre}</strong> · {evento.sucursal.ciudad}
            </p>
            <p className="evento-fecha">
              <time dateTime={evento.fechaHora}>{fechaHoraLegible(evento.fechaHora)}</time>
            </p>
            {evento.nota && <p className="evento-nota">{evento.nota}</p>}
          </li>
        ))}
      </ol>
    </section>
  )
}

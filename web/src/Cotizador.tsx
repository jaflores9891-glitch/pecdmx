import { useEffect, useState, type FormEvent } from 'react'
import { ErrorDeApi, pedir } from './api'
import { kilos, pesos } from './formatos'
import type { Cotizacion, Sucursal } from './tipos'

const CAMPOS_DEL_PAQUETE = [
  { id: 'pesoKg', etiqueta: 'Peso (kg)', ejemplo: '3.2' },
  { id: 'largoCm', etiqueta: 'Largo (cm)', ejemplo: '40' },
  { id: 'anchoCm', etiqueta: 'Ancho (cm)', ejemplo: '30' },
  { id: 'altoCm', etiqueta: 'Alto (cm)', ejemplo: '20' },
] as const

type CampoDelPaquete = (typeof CAMPOS_DEL_PAQUETE)[number]['id']

/** Pestaña "Cotizar": el cliente elige origen, destino y su paquete, y ve cuánto cuesta y por qué. */
export default function Cotizador() {
  const [sucursales, setSucursales] = useState<Sucursal[] | null>(null)
  const [origen, setOrigen] = useState('')
  const [destino, setDestino] = useState('')
  const [paquete, setPaquete] = useState<Record<CampoDelPaquete, string>>({
    pesoKg: '',
    largoCm: '',
    anchoCm: '',
    altoCm: '',
  })
  const [cotizando, setCotizando] = useState(false)
  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null)
  const [error, setError] = useState<string | null>(null)

  // useEffect corre después de pintar la pestaña: aquí se piden las sucursales una sola vez.
  useEffect(() => {
    let vigente = true // si el cliente cambia de pestaña antes de la respuesta, se ignora
    pedir<Sucursal[]>('/api/sucursales')
      .then((lista) => vigente && setSucursales(lista))
      .catch((e: unknown) => vigente && setError(mensaje(e)))
    return () => {
      vigente = false
    }
  }, [])

  const completo = origen !== '' && destino !== '' && Object.values(paquete).every((valor) => valor.trim() !== '')

  async function cotizar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (!completo || cotizando) {
      return
    }
    setCotizando(true)
    setError(null)
    setCotizacion(null)
    try {
      const solicitud = {
        origen,
        destino,
        pesoKg: Number(paquete.pesoKg),
        largoCm: Number(paquete.largoCm),
        anchoCm: Number(paquete.anchoCm),
        altoCm: Number(paquete.altoCm),
      }
      setCotizacion(await pedir<Cotizacion>('/api/cotizaciones', { method: 'POST', body: JSON.stringify(solicitud) }))
    } catch (e) {
      setError(mensaje(e))
    } finally {
      setCotizando(false)
    }
  }

  return (
    <div>
      <p>Calcula cuánto cuesta enviar un paquete entre nuestras sucursales.</p>

      <form className="formulario-cuadricula" onSubmit={cotizar}>
        <SelectorDeSucursal id="origen" etiqueta="Origen" valor={origen} alCambiar={setOrigen} sucursales={sucursales} />
        <SelectorDeSucursal id="destino" etiqueta="Destino" valor={destino} alCambiar={setDestino} sucursales={sucursales} />

        {CAMPOS_DEL_PAQUETE.map(({ id, etiqueta, ejemplo }) => (
          <div key={id}>
            <label htmlFor={id}>{etiqueta}</label>
            <input
              id={id}
              type="number"
              inputMode="decimal"
              min="0"
              step="any"
              placeholder={ejemplo}
              value={paquete[id]}
              onChange={(e) => setPaquete({ ...paquete, [id]: e.target.value })}
            />
          </div>
        ))}

        <button type="submit" className="boton ocupa-toda-la-fila" disabled={!completo || cotizando}>
          {cotizando ? 'Cotizando…' : 'Cotizar'}
        </button>
      </form>

      {error && (
        <p className="alerta" role="alert">
          {error}
        </p>
      )}

      {cotizacion && <Desglose cotizacion={cotizacion} />}
    </div>
  )
}

function mensaje(e: unknown): string {
  return e instanceof ErrorDeApi ? e.message : 'Ocurrió un error inesperado. Intenta de nuevo.'
}

interface PropiedadesDelSelector {
  id: string
  etiqueta: string
  valor: string
  alCambiar: (codigo: string) => void
  sucursales: Sucursal[] | null
}

function SelectorDeSucursal({ id, etiqueta, valor, alCambiar, sucursales }: PropiedadesDelSelector) {
  return (
    <div>
      <label htmlFor={id}>{etiqueta}</label>
      <select id={id} value={valor} onChange={(e) => alCambiar(e.target.value)} disabled={!sucursales}>
        <option value="">{sucursales ? 'Elige una sucursal' : 'Cargando sucursales…'}</option>
        {sucursales?.map((sucursal) => (
          <option key={sucursal.codigo} value={sucursal.codigo}>
            {sucursal.ciudad}
          </option>
        ))}
      </select>
    </div>
  )
}

/** El precio con su explicación: el cliente ve por qué paga lo que paga. */
function Desglose({ cotizacion }: { cotizacion: Cotizacion }) {
  return (
    <section className="resultado" aria-label="Resultado de la cotización">
      <div className="estado-actual">
        <p className="etiqueta">
          Zona {cotizacion.zona} · {cotizacion.descripcionZona}
        </p>
        <p className="ruta">
          {cotizacion.origen.ciudad} → {cotizacion.destino.ciudad}
        </p>
      </div>

      <p className="nota-pesos">
        Peso real {kilos(cotizacion.pesoRealKg)} · Volumétrico {kilos(cotizacion.pesoVolumetricoKg)} · Se cobran{' '}
        <strong>{kilos(cotizacion.pesoCobrableKg)}</strong>
      </p>

      <dl className="desglose">
        <dt>Subtotal</dt>
        <dd>{pesos(cotizacion.subtotal)}</dd>
        <dt>IVA (16 %)</dt>
        <dd>{pesos(cotizacion.iva)}</dd>
        <dt className="total">Total</dt>
        <dd className="total">
          {pesos(cotizacion.total)} {cotizacion.moneda}
        </dd>
      </dl>
    </section>
  )
}

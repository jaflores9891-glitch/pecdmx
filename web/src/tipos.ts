// La forma del JSON que manda el backend. Si el backend cambia, TypeScript avisa en qué partes de la página.

export interface Sucursal {
  codigo: string
  ciudad: string
  estado: string
}

/** Un estado del envío: el código es para programas ("EN_TRANSITO"); nombre y descripción, para personas. */
export interface EstadoEnvio {
  codigo: string
  nombre: string
  descripcion: string
}

export interface EventoDeRastreo {
  estado: EstadoEnvio
  /** En formato ISO-8601 y en UTC, por ejemplo "2026-10-20T15:00:00Z". */
  fechaHora: string
  sucursal: Sucursal
  nota: string
}

/** Respuesta de POST /api/cotizaciones. Montos en pesos mexicanos, antes y después de IVA. */
export interface Cotizacion {
  origen: Sucursal
  destino: Sucursal
  zona: number
  descripcionZona: string
  pesoRealKg: number
  pesoVolumetricoKg: number
  pesoCobrableKg: number
  subtotal: number
  iva: number
  total: number
  moneda: string
}

/** Respuesta de GET /api/guias/{numero}. A propósito no trae el nombre del destinatario ni montos. */
export interface Rastreo {
  numero: string
  origen: Sucursal
  destino: Sucursal
  estado: EstadoEnvio
  historial: EventoDeRastreo[]
}

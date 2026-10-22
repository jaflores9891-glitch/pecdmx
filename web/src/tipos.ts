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

/** Respuesta de GET /api/guias/{numero}. A propósito no trae el nombre del destinatario ni montos. */
export interface Rastreo {
  numero: string
  origen: Sucursal
  destino: Sucursal
  estado: EstadoEnvio
  historial: EventoDeRastreo[]
}

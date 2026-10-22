// El backend manda las horas en UTC; el cliente las ve en hora de la Ciudad de México.
const FORMATO = new Intl.DateTimeFormat('es-MX', {
  timeZone: 'America/Mexico_City',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

/** "2026-10-20T15:00:00Z" → "20 oct 2026, 9:00 a.m." (hora del centro de México). */
export function fechaHoraLegible(isoUtc: string): string {
  return FORMATO.format(new Date(isoUtc))
}

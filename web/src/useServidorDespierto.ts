import { useEffect, useState } from 'react'
import { pedir } from './api'

/**
 * - revisando:   se acaba de preguntar; todavía no se sabe nada.
 * - despertando: el servidor tarda más de lo normal (en Render gratis, se duerme tras 15 min sin uso).
 * - listo:       el servidor contestó.
 * - sin-conexion: no se pudo llegar al servidor.
 */
export type EstadoDelServidor = 'revisando' | 'despertando' | 'listo' | 'sin-conexion'

/** Si el servidor no contesta en este tiempo, probablemente estaba dormido. */
export const ESPERA_ANTES_DE_AVISAR_MS = 3000

/**
 * Al abrir la página, "toca la puerta" del backend con una petición ligera (/api/sucursales).
 * Se usa esa ruta y no /actuator/health porque las reglas de CORS del backend solo cubren /api/**.
 */
export function useServidorDespierto(esperaMs = ESPERA_ANTES_DE_AVISAR_MS): EstadoDelServidor {
  const [estado, setEstado] = useState<EstadoDelServidor>('revisando')

  useEffect(() => {
    let vigente = true
    // Si después de la espera seguimos "revisando", pasamos a "despertando" para mostrar el aviso.
    const temporizador = setTimeout(() => {
      if (vigente) {
        setEstado((actual) => (actual === 'revisando' ? 'despertando' : actual))
      }
    }, esperaMs)

    pedir<unknown>('/api/sucursales')
      .then(() => vigente && setEstado('listo'))
      .catch(() => vigente && setEstado('sin-conexion'))
      .finally(() => clearTimeout(temporizador))

    return () => {
      vigente = false
      clearTimeout(temporizador)
    }
  }, [esperaMs])

  return estado
}

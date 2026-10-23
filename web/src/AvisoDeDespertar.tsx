import { useServidorDespierto } from './useServidorDespierto'

/**
 * Franja que avisa cuando el servidor está despertando (puede tardar hasta 2 minutos)
 * o cuando no hay conexión. Si el servidor contesta rápido, no se muestra nada.
 */
export default function AvisoDeDespertar() {
  const estado = useServidorDespierto()

  if (estado === 'despertando') {
    // role="status": un lector de pantalla lo anuncia sin interrumpir lo que la persona esté haciendo.
    return (
      <div className="aviso aviso-despertando" role="status">
        <span className="girando" aria-hidden="true">
          ⏳
        </span>
        <p>
          <strong>El servidor está despertando…</strong> Usamos un servidor gratuito que se duerme cuando nadie lo usa;
          puede tardar hasta 2 minutos. Ya puedes llenar los datos mientras tanto.
        </p>
      </div>
    )
  }

  if (estado === 'sin-conexion') {
    return (
      <div className="aviso aviso-error" role="alert">
        <p>
          <strong>No pudimos conectar con el servidor.</strong> Revisa tu conexión a internet y recarga la página.
        </p>
      </div>
    )
  }

  return null
}

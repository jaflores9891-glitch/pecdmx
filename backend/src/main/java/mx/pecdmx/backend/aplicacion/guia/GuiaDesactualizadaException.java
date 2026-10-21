package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;

/**
 * Se lanza al guardar una guía cuyo historial cambió desde que se leyó (otro empleado la modificó primero).
 * La solución es volver a leer la guía y repetir la operación.
 */
public class GuiaDesactualizadaException extends RuntimeException {

    public GuiaDesactualizadaException(NumeroDeGuia numero) {
        super("La guía " + numero + " fue modificada por alguien más. Vuelve a consultarla e intenta de nuevo.");
    }
}

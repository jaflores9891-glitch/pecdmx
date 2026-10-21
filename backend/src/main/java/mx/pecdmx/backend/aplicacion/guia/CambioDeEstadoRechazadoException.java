package mx.pecdmx.backend.aplicacion.guia;

/**
 * El cambio de estado choca con cómo va la guía ahora: un salto prohibido, "Entregada" fuera del destino
 * o una fecha anterior al último evento. Envuelve el error del dominio con su mismo mensaje.
 */
public class CambioDeEstadoRechazadoException extends RuntimeException {

    public CambioDeEstadoRechazadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

package mx.pecdmx.backend.aplicacion.guia;

/** A la solicitud le falta un dato que esta operación necesita. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}

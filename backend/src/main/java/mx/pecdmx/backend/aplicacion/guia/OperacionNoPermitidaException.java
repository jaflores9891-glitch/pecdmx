package mx.pecdmx.backend.aplicacion.guia;

/** Se lanza cuando alguien con cuenta intenta algo que su rol o su sucursal no le permiten. */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}

package mx.pecdmx.backend.dominio.tarifa;

/** Se lanza cuando el peso o las medidas de un paquete no cumplen las reglas de PECDMX. */
public class PaqueteInvalidoException extends RuntimeException {

    public PaqueteInvalidoException(String mensaje) {
        super(mensaje);
    }
}

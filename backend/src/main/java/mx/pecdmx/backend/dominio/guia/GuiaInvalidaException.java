package mx.pecdmx.backend.dominio.guia;

/** Se lanza cuando una operación sobre una guía rompe una regla del negocio (sucursal, fecha o datos). */
public class GuiaInvalidaException extends RuntimeException {

    public GuiaInvalidaException(String mensaje) {
        super(mensaje);
    }
}

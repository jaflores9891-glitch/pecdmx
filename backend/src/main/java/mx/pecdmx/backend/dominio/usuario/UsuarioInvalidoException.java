package mx.pecdmx.backend.dominio.usuario;

/** Se lanza cuando los datos de un usuario no cumplen las reglas de PECDMX. */
public class UsuarioInvalidoException extends RuntimeException {

    public UsuarioInvalidoException(String mensaje) {
        super(mensaje);
    }
}

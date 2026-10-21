package mx.pecdmx.backend.aplicacion.usuario;

/**
 * Se lanza cuando el correo o la contraseña no son correctos.
 * El mensaje es el mismo en ambos casos, para no confirmarle a nadie qué correos existen.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }
}

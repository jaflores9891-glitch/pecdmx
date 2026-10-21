package mx.pecdmx.backend.aplicacion.usuario;

/** Se lanza cuando una contraseña nueva no cumple el mínimo de seguridad. */
public class ContrasenaInseguraException extends RuntimeException {

    public ContrasenaInseguraException(int longitudMinima) {
        super("La contraseña debe tener al menos " + longitudMinima + " caracteres.");
    }
}

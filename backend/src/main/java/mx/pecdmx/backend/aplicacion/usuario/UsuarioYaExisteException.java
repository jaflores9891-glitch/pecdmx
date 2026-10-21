package mx.pecdmx.backend.aplicacion.usuario;

import mx.pecdmx.backend.dominio.usuario.Correo;

/** Se lanza al dar de alta un correo que ya tiene cuenta. */
public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(Correo correo) {
        super("Ya existe una cuenta con el correo " + correo + ".");
    }
}

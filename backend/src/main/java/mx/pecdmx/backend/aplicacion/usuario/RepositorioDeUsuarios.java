package mx.pecdmx.backend.aplicacion.usuario;

import java.util.Optional;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/** Puerto de salida: lo que el negocio necesita para guardar y encontrar usuarios. */
public interface RepositorioDeUsuarios {

    Optional<Usuario> buscarPorCorreo(Correo correo);

    void guardar(Usuario usuario);

    /** ¿Existe al menos un usuario? Sirve para crear el gerente inicial solo la primera vez. */
    boolean hayUsuarios();
}

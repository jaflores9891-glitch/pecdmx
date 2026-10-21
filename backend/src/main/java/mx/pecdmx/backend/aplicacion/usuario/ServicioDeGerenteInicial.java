package mx.pecdmx.backend.aplicacion.usuario;

import java.util.Objects;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/**
 * Crea el primer gerente cuando la base de datos todavía no tiene usuarios.
 * Sin él nadie podría iniciar sesión para dar de alta a los demás.
 */
public class ServicioDeGerenteInicial {

    /** Una contraseña larga es más segura que una corta con símbolos raros. */
    public static final int LONGITUD_MINIMA_CONTRASENA = 12;

    private final RepositorioDeUsuarios repositorio;
    private final CifradorDeContrasenas cifrador;

    public ServicioDeGerenteInicial(RepositorioDeUsuarios repositorio, CifradorDeContrasenas cifrador) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.cifrador = Objects.requireNonNull(cifrador);
    }

    /**
     * @return true si lo creó; false si ya había usuarios (no toca nada)
     * @throws ContrasenaInseguraException si la contraseña tiene menos de 12 caracteres
     */
    public boolean crearSiNoHayUsuarios(String correo, String nombre, String contrasena) {
        if (repositorio.hayUsuarios()) {
            return false;
        }
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new ContrasenaInseguraException(LONGITUD_MINIMA_CONTRASENA);
        }
        repositorio.guardar(Usuario.gerente(new Correo(correo), nombre, cifrador.cifrar(contrasena)));
        return true;
    }
}

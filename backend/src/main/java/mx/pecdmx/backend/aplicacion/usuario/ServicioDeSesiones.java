package mx.pecdmx.backend.aplicacion.usuario;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import mx.pecdmx.backend.dominio.usuario.UsuarioInvalidoException;

/** Inicio de sesión: revisa correo y contraseña y, si son correctos, entrega un gafete. */
public class ServicioDeSesiones {

    private final RepositorioDeUsuarios repositorio;
    private final CifradorDeContrasenas cifrador;
    private final EmisorDeGafetes emisor;
    private final Clock reloj;

    /**
     * Huella de una contraseña que nadie tiene. Cuando el correo no existe, se compara contra ella
     * para que la respuesta tarde lo mismo: así nadie puede adivinar qué correos existen midiendo el tiempo.
     */
    private final String huellaDeRelleno;

    public ServicioDeSesiones(
            RepositorioDeUsuarios repositorio, CifradorDeContrasenas cifrador, EmisorDeGafetes emisor, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.emisor = Objects.requireNonNull(emisor);
        this.reloj = Objects.requireNonNull(reloj);
        this.huellaDeRelleno = cifrador.cifrar("relleno-para-igualar-tiempos");
    }

    /** @throws CredencialesInvalidasException si el correo no existe o la contraseña no coincide */
    public Gafete iniciarSesion(String correo, String contrasena) {
        Optional<Usuario> usuario = buscar(correo);
        String huella = usuario.map(Usuario::contrasenaCifrada).orElse(huellaDeRelleno);
        boolean coincide = contrasena != null && cifrador.coincide(contrasena, huella);

        if (usuario.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException();
        }
        return emisor.emitir(usuario.get(), reloj.instant());
    }

    // Un correo mal escrito se trata igual que uno que no existe: mismo mensaje, mismo tiempo.
    private Optional<Usuario> buscar(String correo) {
        try {
            return repositorio.buscarPorCorreo(new Correo(correo));
        } catch (UsuarioInvalidoException correoMalEscrito) {
            return Optional.empty();
        }
    }
}

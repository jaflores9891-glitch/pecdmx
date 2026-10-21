package mx.pecdmx.backend.aplicacion.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Rol;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import mx.pecdmx.backend.dominio.usuario.UsuarioInvalidoException;
import org.junit.jupiter.api.Test;

class ServicioDeGerenteInicialTest {

    /** Repositorio falso en memoria. */
    static class RepositorioEnMemoria implements RepositorioDeUsuarios {
        final Map<Correo, Usuario> usuarios = new HashMap<>();

        @Override
        public Optional<Usuario> buscarPorCorreo(Correo correo) {
            return Optional.ofNullable(usuarios.get(correo));
        }

        @Override
        public void guardar(Usuario usuario) {
            usuarios.put(usuario.correo(), usuario);
        }

        @Override
        public boolean hayUsuarios() {
            return !usuarios.isEmpty();
        }
    }

    /** Cifrador falso: no es seguro, solo sirve para comprobar que el servicio cifra antes de guardar. */
    static class CifradorFalso implements CifradorDeContrasenas {
        @Override
        public String cifrar(String contrasena) {
            return "cifrada:" + contrasena;
        }

        @Override
        public boolean coincide(String contrasena, String contrasenaCifrada) {
            return contrasenaCifrada.equals(cifrar(contrasena));
        }
    }

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final ServicioDeGerenteInicial servicio = new ServicioDeGerenteInicial(repositorio, new CifradorFalso());

    @Test
    void creaElGerenteCuandoNoHayUsuarios() {
        boolean creado = servicio.crearSiNoHayUsuarios("Gerente@PECDMX.test", "Gerente de Prueba", "contrasena-larga-1");

        assertThat(creado).isTrue();
        Usuario gerente = repositorio.buscarPorCorreo(new Correo("gerente@pecdmx.test")).orElseThrow();
        assertThat(gerente.rol()).isEqualTo(Rol.GERENTE);
        assertThat(gerente.contrasenaCifrada())
                .isEqualTo("cifrada:contrasena-larga-1")
                .isNotEqualTo("contrasena-larga-1"); // nunca se guarda la contraseña tal cual
    }

    @Test
    void noHaceNadaSiYaHayUsuarios() {
        servicio.crearSiNoHayUsuarios("primero@pecdmx.test", "Primero", "contrasena-larga-1");

        boolean creado = servicio.crearSiNoHayUsuarios("segundo@pecdmx.test", "Segundo", "contrasena-larga-2");

        assertThat(creado).isFalse();
        assertThat(repositorio.usuarios).hasSize(1);
    }

    @Test
    void rechazaContrasenasDeMenosDe12Caracteres() {
        assertThatThrownBy(() -> servicio.crearSiNoHayUsuarios("gerente@pecdmx.test", "Gerente", "corta-123"))
                .isInstanceOf(ContrasenaInseguraException.class)
                .hasMessageContaining("al menos 12 caracteres");
        assertThat(repositorio.hayUsuarios()).isFalse();
    }

    @Test
    void rechazaUnCorreoInvalido() {
        assertThatThrownBy(() -> servicio.crearSiNoHayUsuarios("no-es-correo", "Gerente", "contrasena-larga-1"))
                .isInstanceOf(UsuarioInvalidoException.class);
    }
}

package mx.pecdmx.backend.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RepositorioDeUsuariosPostgresTest {

    private static final String HUELLA = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";

    @Autowired
    private RepositorioDeUsuarios repositorio;

    @Test
    void guardaYLeeUnEmpleadoConSuSucursal() {
        Usuario empleado = Usuario.empleado(new Correo("luis.mty@pecdmx.test"), "Luis Pérez", Sucursal.MTY, HUELLA);
        repositorio.guardar(empleado);

        assertThat(repositorio.buscarPorCorreo(new Correo("LUIS.MTY@pecdmx.test"))).contains(empleado);
        assertThat(repositorio.hayUsuarios()).isTrue();
    }

    @Test
    void guardaYLeeUnGerenteSinSucursal() {
        Usuario gerente = Usuario.gerente(new Correo("marta.gerente@pecdmx.test"), "Marta Ruiz", HUELLA);
        repositorio.guardar(gerente);

        Usuario leido = repositorio.buscarPorCorreo(gerente.correo()).orElseThrow();
        assertThat(leido.sucursalAsignada()).isEmpty();
    }

    @Test
    void buscarUnCorreoQueNoExisteRegresaVacio() {
        assertThat(repositorio.buscarPorCorreo(new Correo("nadie@pecdmx.test"))).isEmpty();
    }
}

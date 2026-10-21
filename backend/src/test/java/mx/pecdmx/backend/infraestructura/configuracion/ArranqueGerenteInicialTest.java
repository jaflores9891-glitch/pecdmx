package mx.pecdmx.backend.infraestructura.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.usuario.CifradorDeContrasenas;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Rol;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Arranca la aplicación con datos de un gerente de PRUEBA y revisa que se creó con su contraseña cifrada. */
@SpringBootTest(properties = {
    "pecdmx.gerente-inicial.correo=gerente.inicial@pecdmx.test",
    "pecdmx.gerente-inicial.nombre=Gerente de Prueba",
    "pecdmx.gerente-inicial.contrasena=contrasena-solo-para-tests"
})
@Import(TestcontainersConfiguration.class)
class ArranqueGerenteInicialTest {

    @Autowired
    private RepositorioDeUsuarios repositorio;

    @Autowired
    private CifradorDeContrasenas cifrador;

    @Test
    void alArrancarSeCreaElGerenteConSuContrasenaCifradaConBCrypt() {
        Usuario gerente = repositorio.buscarPorCorreo(new Correo("gerente.inicial@pecdmx.test")).orElseThrow();

        assertThat(gerente.rol()).isEqualTo(Rol.GERENTE);
        assertThat(gerente.contrasenaCifrada())
                .startsWith("$2a$") // así empiezan las huellas BCrypt
                .doesNotContain("contrasena-solo-para-tests");
        assertThat(cifrador.coincide("contrasena-solo-para-tests", gerente.contrasenaCifrada())).isTrue();
        assertThat(cifrador.coincide("otra-contrasena", gerente.contrasenaCifrada())).isFalse();
    }
}

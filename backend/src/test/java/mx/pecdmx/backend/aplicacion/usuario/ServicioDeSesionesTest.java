package mx.pecdmx.backend.aplicacion.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicialTest.CifradorFalso;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicialTest.RepositorioEnMemoria;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioDeSesionesTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    /** Emisor falso: un gafete de texto con el correo, que vence en 8 horas. */
    static class EmisorFalso implements EmisorDeGafetes {
        @Override
        public Gafete emitir(Usuario usuario, Instant ahora) {
            return new Gafete("gafete-de:" + usuario.correo(), ahora.plus(Duration.ofHours(8)));
        }
    }

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final CifradorFalso cifrador = new CifradorFalso();
    private final ServicioDeSesiones servicio = new ServicioDeSesiones(
            repositorio, cifrador, new EmisorFalso(), Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void darDeAltaAUnEmpleado() {
        repositorio.guardar(Usuario.empleado(
                new Correo("luis@pecdmx.test"), "Luis Pérez", Sucursal.MTY, cifrador.cifrar("contrasena-de-luis")));
    }

    @Test
    void conCorreoYContrasenaCorrectosEntregaUnGafete() {
        Gafete gafete = servicio.iniciarSesion("LUIS@pecdmx.test", "contrasena-de-luis");

        assertThat(gafete.valor()).isEqualTo("gafete-de:luis@pecdmx.test");
        assertThat(gafete.expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
    }

    @Test
    void conContrasenaEquivocadaLoRechaza() {
        assertThatThrownBy(() -> servicio.iniciarSesion("luis@pecdmx.test", "otra-contrasena"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    void unCorreoQueNoExisteDaElMismoMensaje() {
        assertThatThrownBy(() -> servicio.iniciarSesion("nadie@pecdmx.test", "contrasena-de-luis"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    void unCorreoMalEscritoTambienDaElMismoMensaje() {
        assertThatThrownBy(() -> servicio.iniciarSesion("no-es-correo", "contrasena-de-luis"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void sinContrasenaLoRechaza() {
        assertThatThrownBy(() -> servicio.iniciarSesion("luis@pecdmx.test", null))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    // Para no delatar qué correos existen: con o sin usuario, siempre se compara una contraseña.
    @Test
    void conUnCorreoInexistenteTambienComparaUnaContrasena() {
        ContadorDeComparaciones contador = new ContadorDeComparaciones();
        ServicioDeSesiones conContador = new ServicioDeSesiones(
                repositorio, contador, new EmisorFalso(), Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> conContador.iniciarSesion("nadie@pecdmx.test", "x"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(contador.comparaciones).isEqualTo(1);
    }

    static class ContadorDeComparaciones extends CifradorFalso {
        int comparaciones;

        @Override
        public boolean coincide(String contrasena, String contrasenaCifrada) {
            comparaciones++;
            return super.coincide(contrasena, contrasenaCifrada);
        }
    }
}

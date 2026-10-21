package mx.pecdmx.backend.aplicacion.guia;

import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_SUCURSAL_DESTINO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_TRANSITO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.ENTREGADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioDeCambioDeEstadoTest {

    private static final Instant REGISTRO = Instant.parse("2026-10-21T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-21T17:00:00Z");
    private static final Operador GERENTE = new Operador(new Correo("marta@pecdmx.test"), Rol.GERENTE, null);

    private final RepositorioDeGuiasEnMemoria repositorio = new RepositorioDeGuiasEnMemoria();
    private final CambiarEstadoDeGuia cambiarEstado =
            new ServicioDeCambioDeEstado(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    private String numero;

    private static Operador empleadoDe(Sucursal sucursal) {
        return new Operador(new Correo("empleado@pecdmx.test"), Rol.EMPLEADO, sucursal);
    }

    private SolicitudCambioDeEstado solicitud(EstadoEnvio estado, Sucursal sucursal) {
        return new SolicitudCambioDeEstado(numero, estado, "nota de prueba", sucursal);
    }

    @BeforeEach
    void registrarGuiaCdmxAMty() {
        Cotizacion cotizacion = Cotizacion.calcular(
                Sucursal.CDMX, Sucursal.MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));
        Guia guia = Guia.registrar(repositorio.siguienteNumero(), cotizacion, "Ana López", REGISTRO);
        repositorio.guardar(guia);
        numero = guia.numero().valor();
    }

    @Test
    void unEmpleadoAvanzaLaGuiaEnSuSucursalYSeGuarda() {
        Guia guia = cambiarEstado.cambiar(solicitud(EN_TRANSITO, null), empleadoDe(Sucursal.CDMX));

        assertThat(guia.estadoActual()).isEqualTo(EN_TRANSITO);
        assertThat(guia.ultimoEvento().sucursal()).isEqualTo(Sucursal.CDMX);
        assertThat(guia.ultimoEvento().fechaHora()).isEqualTo(AHORA);
        assertThat(guia.ultimoEvento().nota()).isEqualTo("nota de prueba");
        assertThat(repositorio.buscarPorNumero(guia.numero()).orElseThrow().estadoActual()).isEqualTo(EN_TRANSITO);
    }

    @Test
    void unEmpleadoPuedeRepetirSuPropiaSucursal() {
        assertThat(cambiarEstado.cambiar(solicitud(EN_TRANSITO, Sucursal.CDMX), empleadoDe(Sucursal.CDMX))
                .estadoActual()).isEqualTo(EN_TRANSITO);
    }

    @Test
    void unEmpleadoNoPuedeAnotarEventosEnOtraSucursal() {
        assertThatThrownBy(() -> cambiarEstado.cambiar(solicitud(EN_TRANSITO, Sucursal.PUE), empleadoDe(Sucursal.CDMX)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("tu sucursal (CDMX)");
    }

    @Test
    void unGerenteDebeIndicarLaSucursal() {
        assertThatThrownBy(() -> cambiarEstado.cambiar(solicitud(EN_TRANSITO, null), GERENTE))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("indica la sucursal");
    }

    // QRO no es el origen (CDMX): así se ve que la sucursal sale de lo que indicó el gerente.
    @Test
    void unGerenteAnotaEnLaSucursalQueIndica() {
        Guia guia = cambiarEstado.cambiar(solicitud(EN_TRANSITO, Sucursal.QRO), GERENTE);

        assertThat(guia.estadoActual()).isEqualTo(EN_TRANSITO);
        assertThat(guia.ultimoEvento().sucursal()).isEqualTo(Sucursal.QRO);
    }

    @Test
    void unSaltoProhibidoSeRechazaConElMensajeDelDominio() {
        assertThatThrownBy(() -> cambiarEstado.cambiar(solicitud(ENTREGADA, null), empleadoDe(Sucursal.MTY)))
                .isInstanceOf(CambioDeEstadoRechazadoException.class)
                .hasMessageContaining("no puede pasar a 'Entregada'");
        assertThat(repositorio.buscarPorNumero(new NumeroDeGuia(numero)).orElseThrow().historial()).hasSize(1);
    }

    @Test
    void llegarAlDestinoFueraDelDestinoSeRechaza() {
        cambiarEstado.cambiar(solicitud(EN_TRANSITO, null), empleadoDe(Sucursal.CDMX));

        assertThatThrownBy(() -> cambiarEstado.cambiar(
                        solicitud(EN_SUCURSAL_DESTINO, null), empleadoDe(Sucursal.PUE)))
                .isInstanceOf(CambioDeEstadoRechazadoException.class)
                .hasMessageContaining("sucursal de destino (MTY)");
    }

    @Test
    void unaGuiaQueNoExisteDaGuiaNoEncontrada() {
        numero = repositorio.siguienteNumero().valor();

        assertThatThrownBy(() -> cambiarEstado.cambiar(solicitud(EN_TRANSITO, null), empleadoDe(Sucursal.CDMX)))
                .isInstanceOf(GuiaNoEncontradaException.class);
    }
}

package mx.pecdmx.backend.dominio.guia;

import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.CANCELADA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_SUCURSAL_DESTINO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_TRANSITO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.ENTREGADA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.INCIDENCIA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.REGISTRADA;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.CDMX;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.MTY;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.PUE;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.QRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuiaTest {

    private static final NumeroDeGuia NUMERO = new NumeroDeGuia("PEC2610190049");
    private static final Instant NUEVE_AM = Instant.parse("2026-10-19T15:00:00Z"); // 9:00 en CDMX
    private static final Cotizacion CDMX_A_MTY =
            Cotizacion.calcular(CDMX, MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));

    private Guia guia;

    @BeforeEach
    void registrarGuia() {
        guia = Guia.registrar(NUMERO, CDMX_A_MTY, "  Ana López  ", NUEVE_AM);
    }

    private static Instant horasDespues(int horas) {
        return NUEVE_AM.plus(Duration.ofHours(horas));
    }

    @Test
    void alRegistrarseQuedaRegistradaEnLaSucursalDeOrigen() {
        assertThat(guia.numero()).isEqualTo(NUMERO);
        assertThat(guia.origen()).isEqualTo(CDMX);
        assertThat(guia.destino()).isEqualTo(MTY);
        assertThat(guia.nombreDestinatario()).isEqualTo("Ana López");
        assertThat(guia.cotizacion().total()).isEqualByComparingTo("359.60");
        assertThat(guia.estadoActual()).isEqualTo(REGISTRADA);
        assertThat(guia.historial()).singleElement().satisfies(evento -> {
            assertThat(evento.sucursal()).isEqualTo(CDMX);
            assertThat(evento.fechaHora()).isEqualTo(NUEVE_AM);
        });
    }

    @Test
    void recorridoCompletoHastaLaEntrega() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, "Salió en ruta a Monterrey", horasDespues(2));
        guia.cambiarEstado(EN_SUCURSAL_DESTINO, MTY, "Llegó a Monterrey", horasDespues(14));
        guia.cambiarEstado(ENTREGADA, MTY, "Recogió Ana López", horasDespues(20));

        assertThat(guia.estadoActual()).isEqualTo(ENTREGADA);
        assertThat(guia.historial())
                .extracting(EventoDeRastreo::estado)
                .containsExactly(REGISTRADA, EN_TRANSITO, EN_SUCURSAL_DESTINO, ENTREGADA);
        assertThat(guia.ultimoEvento().nota()).isEqualTo("Recogió Ana López");
    }

    @Test
    void unaIncidenciaPuedeResolverseYSeguirSuCamino() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, null, horasDespues(2));
        guia.cambiarEstado(INCIDENCIA, QRO, "Retraso por bloqueo en carretera", horasDespues(5));
        guia.cambiarEstado(EN_TRANSITO, QRO, "Retomó la ruta", horasDespues(9));

        assertThat(guia.estadoActual()).isEqualTo(EN_TRANSITO);
        assertThat(guia.historial()).hasSize(4);
        assertThat(guia.historial().get(1).nota()).isEmpty(); // una nota nula se guarda vacía
    }

    @Test
    void sePuedeCancelarAntesDeSalir() {
        guia.cambiarEstado(CANCELADA, CDMX, "El cliente canceló", horasDespues(1));

        assertThat(guia.estadoActual()).isEqualTo(CANCELADA);
    }

    @Test
    void rechazaSaltosDeEstadoProhibidosSinTocarElHistorial() {
        assertThatThrownBy(() -> guia.cambiarEstado(ENTREGADA, MTY, null, horasDespues(1)))
                .isInstanceOf(TransicionInvalidaException.class);

        assertThat(guia.historial()).hasSize(1);
    }

    // Regla extra 1: llegar y entregar solo ocurre en la sucursal de destino.
    @Test
    void llegarADestinoSoloOcurreEnLaSucursalDeDestino() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, null, horasDespues(2));

        assertThatThrownBy(() -> guia.cambiarEstado(EN_SUCURSAL_DESTINO, PUE, null, horasDespues(5)))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("sucursal de destino (MTY), no en PUE");
        assertThat(guia.estadoActual()).isEqualTo(EN_TRANSITO);
    }

    @Test
    void entregarSoloOcurreEnLaSucursalDeDestino() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, null, horasDespues(2));
        guia.cambiarEstado(EN_SUCURSAL_DESTINO, MTY, null, horasDespues(14));

        assertThatThrownBy(() -> guia.cambiarEstado(ENTREGADA, CDMX, null, horasDespues(20)))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("no en CDMX");
    }

    // Regla extra 2: el tiempo no va hacia atrás.
    @Test
    void rechazaEventosConFechaAnteriorAlUltimo() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, null, horasDespues(5));

        assertThatThrownBy(() -> guia.cambiarEstado(INCIDENCIA, QRO, null, horasDespues(3)))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("no puede ser anterior");
        assertThat(guia.historial()).hasSize(2);
    }

    @Test
    void permiteDosEventosALaMismaHora() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, null, NUEVE_AM);

        assertThat(guia.estadoActual()).isEqualTo(EN_TRANSITO);
    }

    // Regla extra 3: nombre del destinatario obligatorio y de máximo 100 caracteres.
    @Test
    void exigeElNombreDelDestinatario() {
        assertThatThrownBy(() -> Guia.registrar(NUMERO, CDMX_A_MTY, "   ", NUEVE_AM))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> Guia.registrar(NUMERO, CDMX_A_MTY, null, NUEVE_AM))
                .isInstanceOf(GuiaInvalidaException.class);
    }

    @Test
    void limitaElNombreA100Caracteres() {
        assertThat(Guia.registrar(NUMERO, CDMX_A_MTY, "A".repeat(100), NUEVE_AM).nombreDestinatario())
                .hasSize(100);
        assertThatThrownBy(() -> Guia.registrar(NUMERO, CDMX_A_MTY, "A".repeat(101), NUEVE_AM))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("100 caracteres");
    }

    @Test
    void limitaLaNotaA300Caracteres() {
        assertThatThrownBy(() -> guia.cambiarEstado(EN_TRANSITO, CDMX, "x".repeat(301), horasDespues(1)))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("300 caracteres");
    }

    @Test
    void elHistorialEntregadoNoSePuedeModificarDesdeAfuera() {
        assertThatThrownBy(() -> guia.historial().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(guia.historial()).hasSize(1);
    }

    // --- reconstruir: rearmar una guía guardada ---

    private static EventoDeRastreo evento(EstadoEnvio estado, Sucursal sucursal, int horas) {
        return new EventoDeRastreo(estado, horasDespues(horas), sucursal, "");
    }

    @Test
    void reconstruirDevuelveLaMismaGuiaQueSeGuardo() {
        guia.cambiarEstado(EN_TRANSITO, CDMX, "Salió en ruta", horasDespues(2));
        guia.cambiarEstado(INCIDENCIA, QRO, "Retraso", horasDespues(5));

        Guia rearmada = Guia.reconstruir(NUMERO, CDMX_A_MTY, guia.nombreDestinatario(), guia.historial());

        assertThat(rearmada.numero()).isEqualTo(guia.numero());
        assertThat(rearmada.nombreDestinatario()).isEqualTo("Ana López");
        assertThat(rearmada.estadoActual()).isEqualTo(INCIDENCIA);
        assertThat(rearmada.historial()).isEqualTo(guia.historial());
    }

    @Test
    void unaGuiaReconstruidaSigueAplicandoLasReglas() {
        Guia rearmada = Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López", List.of(evento(REGISTRADA, CDMX, 0)));

        rearmada.cambiarEstado(EN_TRANSITO, CDMX, null, horasDespues(1));
        assertThatThrownBy(() -> rearmada.cambiarEstado(ENTREGADA, MTY, null, horasDespues(2)))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void reconstruirRechazaUnHistorialVacio() {
        assertThatThrownBy(() -> Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López", List.of()))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("PEC2610190049")
                .hasMessageContaining("no tiene eventos");
    }

    @Test
    void reconstruirExigeEmpezarRegistradaEnElOrigen() {
        assertThatThrownBy(() -> Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López",
                        List.of(evento(EN_TRANSITO, CDMX, 0))))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("debe empezar en 'Registrada'");
        assertThatThrownBy(() -> Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López",
                        List.of(evento(REGISTRADA, PUE, 0))))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("sucursal de origen (CDMX)");
    }

    @Test
    void reconstruirDetectaUnSaltoDeEstadoAlterado() {
        List<EventoDeRastreo> alterado = List.of(evento(REGISTRADA, CDMX, 0), evento(ENTREGADA, MTY, 3));

        assertThatThrownBy(() -> Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López", alterado))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("historial guardado de la guía PEC2610190049 no es válido")
                .hasMessageContaining("no puede pasar a 'Entregada'");
    }

    @Test
    void reconstruirDetectaFechasDesordenadas() {
        List<EventoDeRastreo> desordenado = List.of(
                evento(REGISTRADA, CDMX, 5), evento(EN_TRANSITO, CDMX, 2));

        assertThatThrownBy(() -> Guia.reconstruir(NUMERO, CDMX_A_MTY, "Ana López", desordenado))
                .isInstanceOf(GuiaInvalidaException.class)
                .hasMessageContaining("no puede ser anterior");
    }
}

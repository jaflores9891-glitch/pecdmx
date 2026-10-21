package mx.pecdmx.backend.aplicacion.guia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuiaInvalidoException;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioDeRastreoTest {

    private static final Instant NUEVE_AM = Instant.parse("2026-10-20T15:00:00Z");

    private final RepositorioDeGuiasEnMemoria repositorio = new RepositorioDeGuiasEnMemoria();
    private final RastrearGuia rastrearGuia = new ServicioDeRastreo(repositorio);
    private Guia guia;

    @BeforeEach
    void guardarUnaGuiaEnTransito() {
        Cotizacion cotizacion = Cotizacion.calcular(
                Sucursal.CDMX, Sucursal.MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));
        guia = Guia.registrar(repositorio.siguienteNumero(), cotizacion, "Ana López", NUEVE_AM);
        guia.cambiarEstado(EstadoEnvio.EN_TRANSITO, Sucursal.CDMX, "Salió en ruta", NUEVE_AM.plus(Duration.ofHours(2)));
        repositorio.guardar(guia);
    }

    @Test
    void devuelveElEstadoYElHistorialDeLaGuia() {
        Rastreo rastreo = rastrearGuia.rastrear(guia.numero().valor());

        assertThat(rastreo.numero()).isEqualTo(guia.numero());
        assertThat(rastreo.origen()).isEqualTo(Sucursal.CDMX);
        assertThat(rastreo.destino()).isEqualTo(Sucursal.MTY);
        assertThat(rastreo.estadoActual()).isEqualTo(EstadoEnvio.EN_TRANSITO);
        assertThat(rastreo.historial()).isEqualTo(guia.historial());
    }

    @Test
    void aceptaElNumeroComoLoEscribeElCliente() {
        String comoLoEscribe = " " + guia.numero().valor().toLowerCase() + " ";

        assertThat(rastrearGuia.rastrear(comoLoEscribe).numero()).isEqualTo(guia.numero());
    }

    @Test
    void elRastreoNoExponeElNombreDelDestinatario() {
        Rastreo rastreo = rastrearGuia.rastrear(guia.numero().valor());

        assertThat(rastreo.toString()).doesNotContain("Ana López");
    }

    @Test
    void unaGuiaQueNoExisteDaUnErrorClaro() {
        String inexistente = repositorio.siguienteNumero().valor();

        assertThatThrownBy(() -> rastrearGuia.rastrear(inexistente))
                .isInstanceOf(GuiaNoEncontradaException.class)
                .hasMessageContaining("No encontramos la guía " + inexistente);
    }

    @Test
    void unaGuiaMalEscritaSeRechazaSinConsultarElRepositorio() {
        int busquedasAntes = repositorio.busquedas();

        assertThatThrownBy(() -> rastrearGuia.rastrear("PEC2610190042"))
                .isInstanceOf(NumeroDeGuiaInvalidoException.class);
        assertThat(repositorio.busquedas()).isEqualTo(busquedasAntes);
    }
}

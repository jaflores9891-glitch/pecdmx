package mx.pecdmx.backend.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.guia.GuiaDesactualizadaException;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Prueba el adaptador contra un PostgreSQL 17 real que Testcontainers levanta en Docker. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RepositorioDeGuiasPostgresTest {

    // Una hora con nanosegundos, para comprobar que se guarda y se lee igual.
    private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00.123456789Z");

    @Autowired
    private RepositorioDeGuias repositorio;

    // Cada test pide su propio número, así no chocan entre ellos aunque compartan la base de datos.
    private Guia guiaNueva() {
        Cotizacion cotizacion = Cotizacion.calcular(
                Sucursal.CDMX, Sucursal.MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));
        return Guia.registrar(repositorio.siguienteNumero(), cotizacion, "Ana López", AHORA);
    }

    @Test
    void guardaYLeeUnaGuiaIdentica() {
        Guia guia = guiaNueva();
        repositorio.guardar(guia);

        Guia leida = repositorio.buscarPorNumero(guia.numero()).orElseThrow();

        assertThat(leida.numero()).isEqualTo(guia.numero());
        assertThat(leida.nombreDestinatario()).isEqualTo("Ana López");
        assertThat(leida.cotizacion()).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(guia.cotizacion());
        assertThat(leida.cotizacion().pesoRealKg()).isEqualByComparingTo("3.2");
        assertThat(leida.cotizacion().total()).isEqualByComparingTo("359.60");
        assertThat(leida.historial()).isEqualTo(guia.historial());
    }

    @Test
    void alGuardarDeNuevoAgregaLosEventosNuevos() {
        Guia guia = guiaNueva();
        repositorio.guardar(guia);

        guia.cambiarEstado(EstadoEnvio.EN_TRANSITO, Sucursal.CDMX, "Salió en ruta", AHORA.plus(Duration.ofHours(2)));
        guia.cambiarEstado(EstadoEnvio.INCIDENCIA, Sucursal.QRO, "Retraso", AHORA.plus(Duration.ofHours(5)));
        repositorio.guardar(guia);

        Guia leida = repositorio.buscarPorNumero(guia.numero()).orElseThrow();
        assertThat(leida.estadoActual()).isEqualTo(EstadoEnvio.INCIDENCIA);
        assertThat(leida.historial()).isEqualTo(guia.historial());
    }

    @Test
    void guardarSinCambiosNoDuplicaEventos() {
        Guia guia = guiaNueva();
        repositorio.guardar(guia);
        repositorio.guardar(guia);

        assertThat(repositorio.buscarPorNumero(guia.numero()).orElseThrow().historial()).hasSize(1);
    }

    @Test
    void cadaNumeroNuevoEsDistintoYValido() {
        NumeroDeGuia primero = repositorio.siguienteNumero();
        NumeroDeGuia segundo = repositorio.siguienteNumero();

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(NumeroDeGuia.desdeTexto(segundo.valor())).isEqualTo(segundo);
    }

    @Test
    void buscarUnaGuiaQueNoExisteRegresaVacio() {
        assertThat(repositorio.buscarPorNumero(NumeroDeGuia.deSecuencia(999_999_999L))).isEmpty();
    }

    @Test
    void rechazaGuardarUnaCopiaDesactualizada() {
        Guia guia = guiaNueva();
        repositorio.guardar(guia);

        // Dos empleados abren la misma guía al mismo tiempo.
        Guia copiaDeAna = repositorio.buscarPorNumero(guia.numero()).orElseThrow();
        Guia copiaDeLuis = repositorio.buscarPorNumero(guia.numero()).orElseThrow();

        copiaDeAna.cambiarEstado(EstadoEnvio.EN_TRANSITO, Sucursal.CDMX, "Ana", AHORA.plus(Duration.ofHours(1)));
        repositorio.guardar(copiaDeAna);

        copiaDeLuis.cambiarEstado(EstadoEnvio.CANCELADA, Sucursal.CDMX, "Luis", AHORA.plus(Duration.ofHours(1)));
        assertThatThrownBy(() -> repositorio.guardar(copiaDeLuis))
                .isInstanceOf(GuiaDesactualizadaException.class)
                .hasMessageContaining(guia.numero().valor());

        // Se conserva el cambio de Ana: la cancelación de Luis no se guardó.
        assertThat(repositorio.buscarPorNumero(guia.numero()).orElseThrow().estadoActual())
                .isEqualTo(EstadoEnvio.EN_TRANSITO);
    }
}

package mx.pecdmx.backend.dominio.guia;

import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.CANCELADA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_SUCURSAL_DESTINO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.EN_TRANSITO;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.ENTREGADA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.INCIDENCIA;
import static mx.pecdmx.backend.dominio.guia.EstadoEnvio.REGISTRADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoEnvioTest {

    // Los únicos 8 saltos permitidos, escritos a mano a partir de las reglas del negocio.
    private static final Set<String> PERMITIDOS = Set.of(
            "REGISTRADA → EN_TRANSITO",
            "REGISTRADA → CANCELADA",
            "EN_TRANSITO → EN_SUCURSAL_DESTINO",
            "EN_TRANSITO → INCIDENCIA",
            "EN_SUCURSAL_DESTINO → ENTREGADA",
            "EN_SUCURSAL_DESTINO → INCIDENCIA",
            "INCIDENCIA → EN_TRANSITO",
            "INCIDENCIA → EN_SUCURSAL_DESTINO");

    // Las 36 combinaciones (6 × 6), cada una con lo que debería responder.
    static Stream<Arguments> todasLasCombinaciones() {
        return Arrays.stream(EstadoEnvio.values()).flatMap(desde -> Arrays.stream(EstadoEnvio.values())
                .map(hacia -> Arguments.of(desde, hacia, PERMITIDOS.contains(desde + " → " + hacia))));
    }

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @MethodSource("todasLasCombinaciones")
    void soloSePermitenLosSaltosDelNegocio(EstadoEnvio desde, EstadoEnvio hacia, boolean permitido) {
        assertThat(desde.puedePasarA(hacia)).isEqualTo(permitido);
    }

    @Test
    void entregadaYCanceladaSonFinales() {
        assertThat(ENTREGADA.esFinal()).isTrue();
        assertThat(CANCELADA.esFinal()).isTrue();
        assertThat(Stream.of(REGISTRADA, EN_TRANSITO, EN_SUCURSAL_DESTINO, INCIDENCIA))
                .noneMatch(EstadoEnvio::esFinal);
    }

    @Test
    void validarPasoANoFallaCuandoElSaltoEsValido() {
        assertThatCode(() -> REGISTRADA.validarPasoA(EN_TRANSITO)).doesNotThrowAnyException();
    }

    @Test
    void unSaltoProhibidoExplicaCualesSonLosPosibles() {
        assertThatThrownBy(() -> REGISTRADA.validarPasoA(ENTREGADA))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("'Registrada' no puede pasar a 'Entregada'")
                .hasMessageContaining("En tránsito")
                .hasMessageContaining("Cancelada");
    }

    @Test
    void desdeUnEstadoFinalLoDiceClaramente() {
        assertThatThrownBy(() -> ENTREGADA.validarPasoA(EN_TRANSITO))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("es un estado final");
    }

    @Test
    void cadaEstadoTieneNombreYDescripcionParaElCliente() {
        assertThat(EstadoEnvio.values()).allSatisfy(estado -> {
            assertThat(estado.nombre()).isNotBlank();
            assertThat(estado.descripcion()).isNotBlank();
        });
    }
}

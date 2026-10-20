package mx.pecdmx.backend.dominio.guia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NumeroDeGuiaTest {

    @Test
    void aceptaUnaGuiaConSuDigitoVerificadorCorrecto() {
        assertThat(new NumeroDeGuia("PEC2610190049").valor()).isEqualTo("PEC2610190049");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PEC2610190049", "pec2610190049", " PEC 261019004-9 ", "PEC-2610-1900-49"})
    void leeLaGuiaComoLaEscribeElCliente(String texto) {
        assertThat(NumeroDeGuia.desdeTexto(texto)).isEqualTo(new NumeroDeGuia("PEC2610190049"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "1, PEC0000000018",
        "42, PEC0000000422",
        "999999999, PEC9999999999",
    })
    void generaGuiasConSuDigitoVerificador(long secuencia, String esperada) {
        assertThat(NumeroDeGuia.deSecuencia(secuencia).valor()).isEqualTo(esperada);
    }

    @Test
    void todaGuiaGeneradaEsValida() {
        for (long secuencia = 1; secuencia <= 1_000; secuencia++) {
            String valor = NumeroDeGuia.deSecuencia(secuencia).valor();
            assertThat(NumeroDeGuia.desdeTexto(valor).valor()).isEqualTo(valor);
        }
    }

    @ParameterizedTest(name = "rechaza {0}")
    @ValueSource(strings = {
        "PEC2610190042",   // error de dedo en el último dígito
        "PEC2610790049",   // error de dedo en medio
        "PEC2160190049",   // dos dígitos volteados (61 → 16)
    })
    void detectaErroresAlEscribir(String valor) {
        assertThatThrownBy(() -> NumeroDeGuia.desdeTexto(valor))
                .isInstanceOf(NumeroDeGuiaInvalidoException.class)
                .hasMessageContaining("Revisa que esté bien escrito");
    }

    @ParameterizedTest(name = "rechaza [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"PEC261019004", "PEC26101900490", "ABC2610190049", "PECABCDEFGHIJ", "2610190049"})
    void rechazaFormatosIncorrectos(String valor) {
        assertThatThrownBy(() -> NumeroDeGuia.desdeTexto(valor))
                .isInstanceOf(NumeroDeGuiaInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 1_000_000_000L})
    void rechazaSecuenciasFueraDeRango(long secuencia) {
        assertThatThrownBy(() -> NumeroDeGuia.deSecuencia(secuencia))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

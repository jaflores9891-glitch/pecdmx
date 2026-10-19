package mx.pecdmx.backend.dominio.tarifa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaqueteTest {

    private static Paquete paquete(String pesoKg, int largo, int ancho, int alto) {
        return new Paquete(new BigDecimal(pesoKg), largo, ancho, alto);
    }

    @Test
    void ejemploDelNegocio_cajaGrandeYLigeraSeCobraPorVolumen() {
        // 40 × 30 × 20 = 24,000 cm³ ÷ 5,000 = 4.8 kg volumétricos > 3.2 kg reales → se cobran 5 kg
        Paquete caja = paquete("3.2", 40, 30, 20);

        assertThat(caja.pesoVolumetricoKg()).isEqualByComparingTo("4.8");
        assertThat(caja.pesoCobrableKg()).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0} kg, {1}×{2}×{3} cm → se cobran {4} kg")
    @CsvSource({
        "10,   20, 20, 20, 10",  // pesa más de lo que ocupa: se cobra el peso real
        "2.0,  10, 10, 10, 2",   // un kilo exacto no se redondea
        "2.01, 10, 10, 10, 3",   // cualquier fracción sube al kilo siguiente
        "0.3,  10, 10, 10, 1",   // lo mínimo que se cobra es 1 kg
        "70,   10, 10, 10, 70",  // justo en el límite está permitido
    })
    void calculaElPesoCobrable(String peso, int largo, int ancho, int alto, int esperado) {
        assertThat(paquete(peso, largo, ancho, alto).pesoCobrableKg()).isEqualTo(esperado);
    }

    @Test
    void rechazaPaquetesDeMasDe70Kg() {
        assertThatThrownBy(() -> paquete("70.01", 10, 10, 10))
                .isInstanceOf(PaqueteInvalidoException.class)
                .hasMessageContaining("70 kg");
    }

    @Test
    void rechazaPesoCeroNegativoOFaltante() {
        assertThatThrownBy(() -> paquete("0", 10, 10, 10)).isInstanceOf(PaqueteInvalidoException.class);
        assertThatThrownBy(() -> paquete("-1", 10, 10, 10)).isInstanceOf(PaqueteInvalidoException.class);
        assertThatThrownBy(() -> new Paquete(null, 10, 10, 10)).isInstanceOf(PaqueteInvalidoException.class);
    }

    @ParameterizedTest
    @CsvSource({"0, 10, 10", "10, 0, 10", "10, 10, 0", "-5, 10, 10"})
    void rechazaMedidasEnCeroONegativas(int largo, int ancho, int alto) {
        assertThatThrownBy(() -> paquete("1", largo, ancho, alto))
                .isInstanceOf(PaqueteInvalidoException.class)
                .hasMessageContaining("medidas");
    }
}

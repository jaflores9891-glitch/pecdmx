package mx.pecdmx.backend.dominio.tarifa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ZonaTest {

    @ParameterizedTest(name = "{0}: base ${2}, kg extra ${3}")
    @CsvSource({
        "ZONA_1, 1, 90.00, 10.00",
        "ZONA_2, 2, 150.00, 18.00",
        "ZONA_3, 3, 210.00, 25.00",
        "ZONA_4, 4, 290.00, 35.00",
    })
    void cadaZonaTieneSusPrecios(Zona zona, int numero, String base, String kgExtra) {
        assertThat(zona.numero()).isEqualTo(numero);
        assertThat(zona.precioBase()).isEqualByComparingTo(base);
        assertThat(zona.precioPorKgExtra()).isEqualByComparingTo(kgExtra);
    }
}

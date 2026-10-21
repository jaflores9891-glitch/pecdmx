package mx.pecdmx.backend.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PropiedadesJwtTest {

    private static final String SECRETO_DE_PRUEBA = "x".repeat(32);

    // Sin secreto (o con uno corto) el servicio no arranca: Spring crea este objeto al iniciar.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"corto", "solo-31-caracteres-xxxxxxxxxxxx"})
    void sinUnSecretoDeAlMenos32CaracteresNoArranca(String secreto) {
        assertThatThrownBy(() -> new PropiedadesJwt(secreto, Duration.ofHours(8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PECDMX_JWT_SECRETO");
    }

    @Test
    void conUnSecretoDe32CaracteresSiArranca() {
        assertThat(new PropiedadesJwt(SECRETO_DE_PRUEBA, Duration.ofHours(8)).duracion()).hasHours(8);
    }

    @Test
    void laDuracionDebeSerMayorACero() {
        assertThatThrownBy(() -> new PropiedadesJwt(SECRETO_DE_PRUEBA, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void alImprimirseNuncaMuestraElSecreto() {
        assertThat(new PropiedadesJwt(SECRETO_DE_PRUEBA, Duration.ofHours(8)).toString())
                .doesNotContain(SECRETO_DE_PRUEBA);
    }
}

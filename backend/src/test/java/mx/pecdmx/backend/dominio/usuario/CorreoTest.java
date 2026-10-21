package mx.pecdmx.backend.dominio.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CorreoTest {

    @Test
    void seGuardaEnMinusculasYSinEspacios() {
        assertThat(new Correo("  Ana.Lopez@PECDMX.mx ").valor()).isEqualTo("ana.lopez@pecdmx.mx");
    }

    @Test
    void dosCorreosQueSoloDifierenEnMayusculasSonElMismo() {
        assertThat(new Correo("ANA@pecdmx.mx")).isEqualTo(new Correo("ana@PECDMX.MX"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ana", "ana@", "@pecdmx.mx", "ana@pecdmx", "ana lopez@pecdmx.mx", "ana@@pecdmx.mx"})
    void rechazaCorreosSinFormatoValido(String valor) {
        assertThatThrownBy(() -> new Correo(valor)).isInstanceOf(UsuarioInvalidoException.class);
    }

    @Test
    void rechazaCorreosDemasiadoLargos() {
        String largo = "a".repeat(250) + "@pecdmx.mx";

        assertThatThrownBy(() -> new Correo(largo))
                .isInstanceOf(UsuarioInvalidoException.class)
                .hasMessageContaining("formato válido");
    }
}

package mx.pecdmx.backend.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PropiedadesCorsTest {

    @Test
    void aceptaVariosOrigenesYLesQuitaEspacios() {
        PropiedadesCors cors = new PropiedadesCors(List.of(" https://pecdmx.vercel.app", "http://localhost:5173 "));

        assertThat(cors.origenes()).containsExactly("https://pecdmx.vercel.app", "http://localhost:5173");
    }

    @Test
    void sinOrigenesQuedaLaListaVacia() {
        assertThat(new PropiedadesCors(null).origenes()).isEmpty();
    }

    @Test
    void rechazaElComodinQueDejariaPasarACualquierPagina() {
        assertThatThrownBy(() -> new PropiedadesCors(List.of("*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cualquier página");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pecdmx.vercel.app", "https://pecdmx.vercel.app/", "https://pecdmx.vercel.app/inicio", "ftp://pecdmx.mx"})
    void rechazaOrigenesMalEscritos(String origen) {
        assertThatThrownBy(() -> new PropiedadesCors(List.of(origen)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sin / al final");
    }
}

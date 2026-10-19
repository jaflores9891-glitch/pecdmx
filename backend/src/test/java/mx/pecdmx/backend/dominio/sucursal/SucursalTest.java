package mx.pecdmx.backend.dominio.sucursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SucursalTest {

    @Test
    void existenLasSeisSucursales() {
        assertThat(Sucursal.values())
                .extracting(Sucursal::codigo)
                .containsExactly("CDMX", "GDL", "MTY", "PUE", "QRO", "TIJ");
    }

    @Test
    void cadaSucursalTieneCiudadYEstado() {
        assertThat(Sucursal.MTY.ciudad()).isEqualTo("Monterrey");
        assertThat(Sucursal.MTY.estado()).isEqualTo("Nuevo León");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MTY", "mty", " Mty "})
    void convierteElCodigoSinImportarMayusculasNiEspacios(String codigo) {
        assertThat(Sucursal.desdeCodigo(codigo)).isEqualTo(Sucursal.MTY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "XYZ", "Monterrey"})
    void rechazaCodigosQueNoExisten(String codigo) {
        assertThatThrownBy(() -> Sucursal.desdeCodigo(codigo))
                .isInstanceOf(SucursalDesconocidaException.class)
                .hasMessageContaining("Sucursales válidas");
    }
}

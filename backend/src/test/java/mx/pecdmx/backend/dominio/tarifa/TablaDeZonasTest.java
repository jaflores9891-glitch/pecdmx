package mx.pecdmx.backend.dominio.tarifa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TablaDeZonasTest {

    @ParameterizedTest
    @EnumSource(Sucursal.class)
    void laMismaCiudadEsZona1(Sucursal sucursal) {
        assertThat(TablaDeZonas.zonaEntre(sucursal, sucursal)).isEqualTo(Zona.ZONA_1);
    }

    // Los 15 pares posibles entre sucursales distintas.
    @ParameterizedTest(name = "{0} ↔ {1} = {2}")
    @CsvSource({
        "CDMX, PUE, ZONA_2",
        "CDMX, QRO, ZONA_2",
        "PUE,  QRO, ZONA_2",
        "GDL,  QRO, ZONA_2",
        "CDMX, GDL, ZONA_3",
        "CDMX, MTY, ZONA_3",
        "GDL,  MTY, ZONA_3",
        "MTY,  QRO, ZONA_3",
        "GDL,  PUE, ZONA_3",
        "MTY,  PUE, ZONA_3",
        "TIJ, CDMX, ZONA_4",
        "TIJ,  GDL, ZONA_4",
        "TIJ,  MTY, ZONA_4",
        "TIJ,  PUE, ZONA_4",
        "TIJ,  QRO, ZONA_4",
    })
    void cadaParTieneSuZonaEnAmbosSentidos(Sucursal a, Sucursal b, Zona esperada) {
        assertThat(TablaDeZonas.zonaEntre(a, b)).isEqualTo(esperada);
        assertThat(TablaDeZonas.zonaEntre(b, a)).isEqualTo(esperada);
    }

    @Test
    void exigeOrigenYDestino() {
        assertThatThrownBy(() -> TablaDeZonas.zonaEntre(null, Sucursal.MTY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("origen");
        assertThatThrownBy(() -> TablaDeZonas.zonaEntre(Sucursal.MTY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("destino");
    }
}

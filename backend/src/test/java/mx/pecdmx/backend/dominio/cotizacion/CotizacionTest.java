package mx.pecdmx.backend.dominio.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import mx.pecdmx.backend.dominio.tarifa.Zona;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CotizacionTest {

    private static Paquete paquete(String pesoKg, int largo, int ancho, int alto) {
        return new Paquete(new BigDecimal(pesoKg), largo, ancho, alto);
    }

    @Test
    void ejemploDelNegocio_cdmxAMonterreyConCajaDe5KgCobrables() {
        Cotizacion cotizacion = Cotizacion.calcular(Sucursal.CDMX, Sucursal.MTY, paquete("3.2", 40, 30, 20));

        assertThat(cotizacion.zona()).isEqualTo(Zona.ZONA_3);
        assertThat(cotizacion.pesoRealKg()).isEqualByComparingTo("3.2");
        assertThat(cotizacion.pesoVolumetricoKg()).isEqualByComparingTo("4.8");
        assertThat(cotizacion.pesoCobrableKg()).isEqualTo(5);
        assertThat(cotizacion.subtotal()).isEqualByComparingTo("310.00");  // 210 + 4 × 25
        assertThat(cotizacion.iva()).isEqualByComparingTo("49.60");
        assertThat(cotizacion.total()).isEqualByComparingTo("359.60");
    }

    @ParameterizedTest(name = "{0} → {1}, {2} kg: subtotal ${3}, IVA ${4}, total ${5}")
    @CsvSource({
        "CDMX, CDMX,  1,  90.00,  14.40,  104.40",   // 1 kg: solo el precio base
        "PUE,  CDMX,  2, 168.00,  26.88,  194.88",   // zona 2: 150 + 1 × 18
        "GDL,  QRO,  10, 312.00,  49.92,  361.92",   // zona 2: 150 + 9 × 18
        "TIJ,  PUE,  70, 2705.00, 432.80, 3137.80",  // el envío más caro posible: 290 + 69 × 35
    })
    void aplicaLaFormulaDePrecioYElIva(
            Sucursal origen, Sucursal destino, String kg, String subtotal, String iva, String total) {
        Cotizacion cotizacion = Cotizacion.calcular(origen, destino, paquete(kg, 10, 10, 10));

        assertThat(cotizacion.subtotal()).isEqualByComparingTo(subtotal);
        assertThat(cotizacion.iva()).isEqualByComparingTo(iva);
        assertThat(cotizacion.total()).isEqualByComparingTo(total);
    }

    @Test
    void elTotalEsSiempreSubtotalMasIva() {
        Cotizacion cotizacion = Cotizacion.calcular(Sucursal.GDL, Sucursal.TIJ, paquete("12.7", 50, 40, 30));

        assertThat(cotizacion.total()).isEqualByComparingTo(cotizacion.subtotal().add(cotizacion.iva()));
    }

    @Test
    void losMontosSiempreTienenDosDecimales() {
        Cotizacion cotizacion = Cotizacion.calcular(Sucursal.QRO, Sucursal.MTY, paquete("1", 10, 10, 10));

        assertThat(cotizacion.subtotal().scale()).isEqualTo(2);
        assertThat(cotizacion.iva().scale()).isEqualTo(2);
        assertThat(cotizacion.total().scale()).isEqualTo(2);
    }

    @Test
    void exigeUnPaquete() {
        assertThatThrownBy(() -> Cotizacion.calcular(Sucursal.CDMX, Sucursal.MTY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paquete");
    }
}

package mx.pecdmx.backend.aplicacion.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.sucursal.SucursalDesconocidaException;
import mx.pecdmx.backend.dominio.tarifa.PaqueteInvalidoException;
import org.junit.jupiter.api.Test;

class ServicioDeCotizacionTest {

    // Se usa a través del puerto, igual que lo harán la API y el chatbot.
    private final CotizarEnvio cotizarEnvio = new ServicioDeCotizacion();

    private static SolicitudCotizacion solicitud(String origen, String destino, String pesoKg) {
        return new SolicitudCotizacion(origen, destino, new BigDecimal(pesoKg), 40, 30, 20);
    }

    @Test
    void cotizaConLosDatosTalComoLosEscribeElCliente() {
        Cotizacion cotizacion = cotizarEnvio.cotizar(solicitud(" cdmx", "Mty ", "3.2"));

        assertThat(cotizacion.origen()).isEqualTo(Sucursal.CDMX);
        assertThat(cotizacion.destino()).isEqualTo(Sucursal.MTY);
        assertThat(cotizacion.pesoCobrableKg()).isEqualTo(5);
        assertThat(cotizacion.total()).isEqualByComparingTo("359.60");
    }

    @Test
    void avisaCuandoElOrigenNoExiste() {
        assertThatThrownBy(() -> cotizarEnvio.cotizar(solicitud("XYZ", "MTY", "3.2")))
                .isInstanceOf(SucursalDesconocidaException.class)
                .hasMessageContaining("'XYZ'");
    }

    @Test
    void avisaCuandoElDestinoNoExiste() {
        assertThatThrownBy(() -> cotizarEnvio.cotizar(solicitud("CDMX", "Cancún", "3.2")))
                .isInstanceOf(SucursalDesconocidaException.class)
                .hasMessageContaining("'Cancún'");
    }

    @Test
    void avisaCuandoElPaqueteNoEsValido() {
        assertThatThrownBy(() -> cotizarEnvio.cotizar(solicitud("CDMX", "MTY", "80")))
                .isInstanceOf(PaqueteInvalidoException.class)
                .hasMessageContaining("70 kg");
    }

    @Test
    void exigeUnaSolicitud() {
        assertThatThrownBy(() -> cotizarEnvio.cotizar(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("solicitud");
    }
}

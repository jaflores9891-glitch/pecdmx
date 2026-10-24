package mx.pecdmx.backend.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Las guías de demostración (migración V3) que ve quien visita la página.
 * Al leerlas, el dominio revisa su número (Luhn) y que el historial respete las transiciones;
 * además, sus montos deben ser los que calcula HOY el cotizador: si cambian las tarifas, este test avisa.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GuiasDeDemostracionTest {

    @Autowired
    private RepositorioDeGuias repositorio;

    @ParameterizedTest(name = "{0} está {1}")
    @CsvSource({
        // numero,       estado,              eventos, peso, largo, ancho, alto
        "PEC9000000019, ENTREGADA,            4, 3.5, 40, 30, 20",
        "PEC9000000027, EN_TRANSITO,          2, 8,   50, 40, 30",
        "PEC9000000035, EN_SUCURSAL_DESTINO,  3, 1.2, 25, 20, 10",
        "PEC9000000043, REGISTRADA,           1, 12,  60, 40, 40",
        "PEC9000000050, INCIDENCIA,           3, 5,   40, 40, 30",
        "PEC9000000068, CANCELADA,            2, 2,   30, 20, 15",
    })
    void cadaGuiaDeDemostracionEsValidaYCuadraConElCotizador(
            String numero, EstadoEnvio estado, int eventos, BigDecimal peso, int largo, int ancho, int alto) {
        Guia guia = repositorio.buscarPorNumero(new NumeroDeGuia(numero)).orElseThrow();

        assertThat(guia.estadoActual()).isEqualTo(estado);
        assertThat(guia.historial()).hasSize(eventos);

        Cotizacion hoy = Cotizacion.calcular(guia.origen(), guia.destino(), new Paquete(peso, largo, ancho, alto));
        assertThat(guia.cotizacion().total()).isEqualByComparingTo(hoy.total());
        assertThat(guia.cotizacion().iva()).isEqualByComparingTo(hoy.iva());
    }
}

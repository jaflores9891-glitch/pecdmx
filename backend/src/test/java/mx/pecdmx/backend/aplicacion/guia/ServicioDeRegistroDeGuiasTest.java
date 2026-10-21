package mx.pecdmx.backend.aplicacion.guia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import mx.pecdmx.backend.aplicacion.cotizacion.ServicioDeCotizacion;
import mx.pecdmx.backend.aplicacion.cotizacion.SolicitudCotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Rol;
import org.junit.jupiter.api.Test;

class ServicioDeRegistroDeGuiasTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T16:00:00Z");
    private static final Operador EMPLEADO_CDMX =
            new Operador(new Correo("ana@pecdmx.test"), Rol.EMPLEADO, Sucursal.CDMX);
    private static final Operador GERENTE = new Operador(new Correo("marta@pecdmx.test"), Rol.GERENTE, null);

    private final RepositorioDeGuiasEnMemoria repositorio = new RepositorioDeGuiasEnMemoria();
    private final RegistrarGuia registrarGuia = new ServicioDeRegistroDeGuias(
            new ServicioDeCotizacion(), repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

    private static SolicitudRegistroDeGuia solicitud(String origen, String destino) {
        return new SolicitudRegistroDeGuia(
                new SolicitudCotizacion(origen, destino, new BigDecimal("3.2"), 40, 30, 20), "Ana López");
    }

    @Test
    void unEmpleadoRegistraUnaGuiaQueSaleDeSuSucursal() {
        Guia guia = registrarGuia.registrar(solicitud("cdmx", "mty"), EMPLEADO_CDMX);

        assertThat(guia.origen()).isEqualTo(Sucursal.CDMX);
        assertThat(guia.destino()).isEqualTo(Sucursal.MTY);
        assertThat(guia.nombreDestinatario()).isEqualTo("Ana López");
        assertThat(guia.cotizacion().total()).isEqualByComparingTo("359.60");
        assertThat(guia.estadoActual()).isEqualTo(EstadoEnvio.REGISTRADA);
        assertThat(guia.ultimoEvento().fechaHora()).isEqualTo(AHORA);
        assertThat(repositorio.buscarPorNumero(guia.numero())).contains(guia);
    }

    @Test
    void cadaGuiaRecibeUnNumeroDistinto() {
        Guia primera = registrarGuia.registrar(solicitud("CDMX", "MTY"), EMPLEADO_CDMX);
        Guia segunda = registrarGuia.registrar(solicitud("CDMX", "PUE"), EMPLEADO_CDMX);

        assertThat(primera.numero()).isNotEqualTo(segunda.numero());
    }

    @Test
    void unEmpleadoNoPuedeRegistrarGuiasDeOtraSucursalNiGastaUnNumero() {
        assertThatThrownBy(() -> registrarGuia.registrar(solicitud("MTY", "CDMX"), EMPLEADO_CDMX))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("origen en tu sucursal (CDMX)");

        // El siguiente número sigue siendo el 1: el intento rechazado no consumió ninguno.
        assertThat(registrarGuia.registrar(solicitud("CDMX", "MTY"), EMPLEADO_CDMX).numero().valor())
                .isEqualTo("PEC0000000018");
    }

    @Test
    void unGerenteRegistraDesdeCualquierSucursal() {
        assertThat(registrarGuia.registrar(solicitud("TIJ", "GDL"), GERENTE).origen()).isEqualTo(Sucursal.TIJ);
    }
}

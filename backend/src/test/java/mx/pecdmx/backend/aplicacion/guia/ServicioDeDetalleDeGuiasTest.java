package mx.pecdmx.backend.aplicacion.guia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ServicioDeDetalleDeGuiasTest {

    private final RepositorioDeGuiasEnMemoria repositorio = new RepositorioDeGuiasEnMemoria();
    private final ConsultarDetalleDeGuia consultar = new ServicioDeDetalleDeGuias(repositorio);
    private Guia guiaCdmxAMty;

    private static Operador empleadoDe(Sucursal sucursal) {
        return new Operador(new Correo("empleado@pecdmx.test"), Rol.EMPLEADO, sucursal);
    }

    @BeforeEach
    void guardarGuia() {
        Cotizacion cotizacion = Cotizacion.calcular(
                Sucursal.CDMX, Sucursal.MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));
        guiaCdmxAMty = Guia.registrar(
                repositorio.siguienteNumero(), cotizacion, "Ana López", Instant.parse("2026-10-21T16:00:00Z"));
        repositorio.guardar(guiaCdmxAMty);
    }

    @ParameterizedTest
    @EnumSource(value = Sucursal.class, names = {"CDMX", "MTY"})
    void laVenLosEmpleadosDelOrigenYDelDestino(Sucursal sucursal) {
        Guia detalle = consultar.consultar(guiaCdmxAMty.numero().valor(), empleadoDe(sucursal));

        assertThat(detalle.nombreDestinatario()).isEqualTo("Ana López");
    }

    @ParameterizedTest
    @EnumSource(value = Sucursal.class, names = {"GDL", "PUE", "QRO", "TIJ"})
    void noLaVenLosEmpleadosDeOtrasSucursales(Sucursal sucursal) {
        assertThatThrownBy(() -> consultar.consultar(guiaCdmxAMty.numero().valor(), empleadoDe(sucursal)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("salen de tu sucursal o llegan a ella");
    }

    @Test
    void unGerenteLaVe() {
        Operador gerente = new Operador(new Correo("marta@pecdmx.test"), Rol.GERENTE, null);

        assertThat(consultar.consultar(guiaCdmxAMty.numero().valor(), gerente).numero())
                .isEqualTo(guiaCdmxAMty.numero());
    }

    @Test
    void unaGuiaQueNoExisteDaGuiaNoEncontrada() {
        String inexistente = repositorio.siguienteNumero().valor();

        assertThatThrownBy(() -> consultar.consultar(inexistente, empleadoDe(Sucursal.CDMX)))
                .isInstanceOf(GuiaNoEncontradaException.class);
    }
}

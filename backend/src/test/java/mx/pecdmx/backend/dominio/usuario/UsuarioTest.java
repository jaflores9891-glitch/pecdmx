package mx.pecdmx.backend.dominio.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UsuarioTest {

    // Una "huella" BCrypt de ejemplo. No corresponde a ninguna contraseña real.
    private static final String HUELLA = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";
    private static final Correo CORREO = new Correo("luis@pecdmx.mx");

    private final Usuario empleadoDeMonterrey = Usuario.empleado(CORREO, "Luis Pérez", Sucursal.MTY, HUELLA);
    private final Usuario gerente = Usuario.gerente(new Correo("marta@pecdmx.mx"), "Marta Ruiz", HUELLA);

    @Test
    void unEmpleadoPerteneceASuSucursal() {
        assertThat(empleadoDeMonterrey.rol()).isEqualTo(Rol.EMPLEADO);
        assertThat(empleadoDeMonterrey.sucursalAsignada()).contains(Sucursal.MTY);
    }

    @Test
    void unGerenteNoTieneSucursalAsignada() {
        assertThat(gerente.rol()).isEqualTo(Rol.GERENTE);
        assertThat(gerente.sucursalAsignada()).isEmpty();
    }

    @Test
    void unEmpleadoSoloOperaEnSuSucursal() {
        assertThat(empleadoDeMonterrey.puedeOperarEn(Sucursal.MTY)).isTrue();
        assertThat(empleadoDeMonterrey.puedeOperarEn(Sucursal.CDMX)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Sucursal.class)
    void unGerenteOperaEnCualquierSucursal(Sucursal sucursal) {
        assertThat(gerente.puedeOperarEn(sucursal)).isTrue();
    }

    @Test
    void unEmpleadoSinSucursalNoEsValido() {
        assertThatThrownBy(() -> Usuario.empleado(CORREO, "Luis Pérez", null, HUELLA))
                .isInstanceOf(UsuarioInvalidoException.class)
                .hasMessageContaining("debe pertenecer a una sucursal");
    }

    @Test
    void unGerenteConSucursalNoEsValido() {
        assertThatThrownBy(() -> new Usuario(CORREO, "Marta Ruiz", Rol.GERENTE, Sucursal.CDMX, HUELLA))
                .isInstanceOf(UsuarioInvalidoException.class)
                .hasMessageContaining("todas las sucursales");
    }

    @Test
    void exigeNombreDeMaximo100Caracteres() {
        assertThatThrownBy(() -> Usuario.empleado(CORREO, "  ", Sucursal.MTY, HUELLA))
                .isInstanceOf(UsuarioInvalidoException.class)
                .hasMessageContaining("nombre es obligatorio");
        assertThatThrownBy(() -> Usuario.empleado(CORREO, "L".repeat(101), Sucursal.MTY, HUELLA))
                .isInstanceOf(UsuarioInvalidoException.class)
                .hasMessageContaining("100 caracteres");
    }

    @Test
    void exigeLaContrasenaCifrada() {
        assertThatThrownBy(() -> Usuario.empleado(CORREO, "Luis Pérez", Sucursal.MTY, " "))
                .isInstanceOf(UsuarioInvalidoException.class);
    }

    @Test
    void alImprimirseNuncaMuestraLaContrasenaCifrada() {
        assertThat(empleadoDeMonterrey.toString())
                .contains("luis@pecdmx.mx", "EMPLEADO", "MTY")
                .doesNotContain(HUELLA);
    }
}

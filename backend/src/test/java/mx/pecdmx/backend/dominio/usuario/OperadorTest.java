package mx.pecdmx.backend.dominio.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OperadorTest {

    private static final Correo CORREO = new Correo("luis@pecdmx.test");

    @Test
    void unEmpleadoSoloOperaEnSuSucursal() {
        Operador empleado = new Operador(CORREO, Rol.EMPLEADO, Sucursal.CDMX);

        assertThat(empleado.esGerente()).isFalse();
        assertThat(empleado.puedeOperarEn(Sucursal.CDMX)).isTrue();
        assertThat(empleado.puedeOperarEn(Sucursal.MTY)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Sucursal.class)
    void unGerenteOperaEnTodas(Sucursal sucursal) {
        assertThat(new Operador(CORREO, Rol.GERENTE, null).puedeOperarEn(sucursal)).isTrue();
    }

    @Test
    void respetaLasMismasReglasDeRolYSucursalQueUnUsuario() {
        assertThatThrownBy(() -> new Operador(CORREO, Rol.EMPLEADO, null)).isInstanceOf(UsuarioInvalidoException.class);
        assertThatThrownBy(() -> new Operador(CORREO, Rol.GERENTE, Sucursal.PUE))
                .isInstanceOf(UsuarioInvalidoException.class);
    }

    @Test
    void unUsuarioSeConvierteEnSuOperador() {
        Usuario usuario = Usuario.empleado(CORREO, "Luis Pérez", Sucursal.QRO, "huella");

        assertThat(usuario.comoOperador()).isEqualTo(new Operador(CORREO, Rol.EMPLEADO, Sucursal.QRO));
    }
}

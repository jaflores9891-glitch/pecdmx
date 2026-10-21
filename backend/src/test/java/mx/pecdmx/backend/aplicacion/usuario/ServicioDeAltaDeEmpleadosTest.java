package mx.pecdmx.backend.aplicacion.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.pecdmx.backend.aplicacion.guia.OperacionNoPermitidaException;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicialTest.CifradorFalso;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicialTest.RepositorioEnMemoria;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Rol;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import mx.pecdmx.backend.dominio.usuario.UsuarioInvalidoException;
import org.junit.jupiter.api.Test;

class ServicioDeAltaDeEmpleadosTest {

    private static final Operador GERENTE = new Operador(new Correo("marta@pecdmx.test"), Rol.GERENTE, null);
    private static final Operador EMPLEADO = new Operador(new Correo("ana@pecdmx.test"), Rol.EMPLEADO, Sucursal.CDMX);

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final DarDeAltaEmpleado darDeAlta = new ServicioDeAltaDeEmpleados(repositorio, new CifradorFalso());

    private static SolicitudAltaDeEmpleado solicitud(String correo, String contrasena) {
        return new SolicitudAltaDeEmpleado(correo, "Luis Pérez", Sucursal.MTY, contrasena);
    }

    @Test
    void unGerenteDaDeAltaAUnEmpleadoConSuContrasenaCifrada() {
        Usuario empleado = darDeAlta.darDeAlta(solicitud("Luis@PECDMX.test", "contrasena-inicial-1"), GERENTE);

        assertThat(empleado.rol()).isEqualTo(Rol.EMPLEADO);
        assertThat(empleado.sucursalAsignada()).contains(Sucursal.MTY);
        assertThat(empleado.contrasenaCifrada()).isEqualTo("cifrada:contrasena-inicial-1");
        assertThat(repositorio.buscarPorCorreo(new Correo("luis@pecdmx.test"))).contains(empleado);
    }

    @Test
    void unEmpleadoNoPuedeDarDeAltaAOtros() {
        assertThatThrownBy(() -> darDeAlta.darDeAlta(solicitud("luis@pecdmx.test", "contrasena-inicial-1"), EMPLEADO))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Solo un gerente");
        assertThat(repositorio.hayUsuarios()).isFalse();
    }

    @Test
    void unCorreoRepetidoSeRechazaAunqueCambienLasMayusculas() {
        darDeAlta.darDeAlta(solicitud("luis@pecdmx.test", "contrasena-inicial-1"), GERENTE);

        assertThatThrownBy(() -> darDeAlta.darDeAlta(solicitud("LUIS@pecdmx.test", "otra-contrasena-2"), GERENTE))
                .isInstanceOf(UsuarioYaExisteException.class)
                .hasMessageContaining("luis@pecdmx.test");
    }

    @Test
    void exigeUnaContrasenaInicialDeAlMenos12Caracteres() {
        assertThatThrownBy(() -> darDeAlta.darDeAlta(solicitud("luis@pecdmx.test", "corta-123"), GERENTE))
                .isInstanceOf(ContrasenaInseguraException.class);
        assertThat(repositorio.hayUsuarios()).isFalse();
    }

    @Test
    void rechazaUnCorreoInvalido() {
        assertThatThrownBy(() -> darDeAlta.darDeAlta(solicitud("luis-sin-arroba", "contrasena-inicial-1"), GERENTE))
                .isInstanceOf(UsuarioInvalidoException.class);
    }
}

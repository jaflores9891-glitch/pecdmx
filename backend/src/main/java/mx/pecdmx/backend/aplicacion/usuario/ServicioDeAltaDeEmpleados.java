package mx.pecdmx.backend.aplicacion.usuario;

import java.util.Objects;
import mx.pecdmx.backend.aplicacion.guia.OperacionNoPermitidaException;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/**
 * Da de alta empleados. El guardia de la API ya exige rol de gerente, pero el caso de uso lo vuelve a revisar:
 * así el negocio queda protegido aunque alguien cambie por error las reglas del guardia.
 */
public class ServicioDeAltaDeEmpleados implements DarDeAltaEmpleado {

    private final RepositorioDeUsuarios repositorio;
    private final CifradorDeContrasenas cifrador;

    public ServicioDeAltaDeEmpleados(RepositorioDeUsuarios repositorio, CifradorDeContrasenas cifrador) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.cifrador = Objects.requireNonNull(cifrador);
    }

    @Override
    public Usuario darDeAlta(SolicitudAltaDeEmpleado solicitud, Operador operador) {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
        Objects.requireNonNull(operador, "El operador es obligatorio");
        if (!operador.esGerente()) {
            throw new OperacionNoPermitidaException("Solo un gerente puede dar de alta empleados.");
        }

        Correo correo = new Correo(solicitud.correo());
        if (repositorio.buscarPorCorreo(correo).isPresent()) {
            throw new UsuarioYaExisteException(correo);
        }
        String contrasena = solicitud.contrasenaInicial();
        if (contrasena == null || contrasena.length() < ServicioDeGerenteInicial.LONGITUD_MINIMA_CONTRASENA) {
            throw new ContrasenaInseguraException(ServicioDeGerenteInicial.LONGITUD_MINIMA_CONTRASENA);
        }

        Usuario empleado = Usuario.empleado(correo, solicitud.nombre(), solicitud.sucursal(), cifrador.cifrar(contrasena));
        repositorio.guardar(empleado);
        return empleado;
    }
}

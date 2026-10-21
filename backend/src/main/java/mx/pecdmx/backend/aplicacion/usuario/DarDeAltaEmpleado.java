package mx.pecdmx.backend.aplicacion.usuario;

import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/** Puerto de entrada: un gerente crea la cuenta de un empleado de sucursal. */
public interface DarDeAltaEmpleado {

    /**
     * @throws mx.pecdmx.backend.aplicacion.guia.OperacionNoPermitidaException si quien lo pide no es gerente
     * @throws UsuarioYaExisteException si ya hay una cuenta con ese correo
     * @throws ContrasenaInseguraException si la contraseña inicial tiene menos de 12 caracteres
     * @throws mx.pecdmx.backend.dominio.usuario.UsuarioInvalidoException si el correo o el nombre no son válidos
     */
    Usuario darDeAlta(SolicitudAltaDeEmpleado solicitud, Operador operador);
}

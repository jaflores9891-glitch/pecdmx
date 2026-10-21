package mx.pecdmx.backend.aplicacion.usuario;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/** Datos para dar de alta a un empleado. La contraseña inicial se la comunica el gerente por fuera del sistema. */
public record SolicitudAltaDeEmpleado(String correo, String nombre, Sucursal sucursal, String contrasenaInicial) {

    /** Nunca imprime la contraseña inicial. */
    @Override
    public String toString() {
        return "SolicitudAltaDeEmpleado[correo=" + correo + ", nombre=" + nombre + ", sucursal=" + sucursal + "]";
    }
}

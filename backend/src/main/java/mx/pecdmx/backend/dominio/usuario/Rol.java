package mx.pecdmx.backend.dominio.usuario;

/**
 * Roles de las personas con cuenta. El cliente no tiene cuenta: es quien usa la parte pública sin iniciar sesión.
 */
public enum Rol {

    /** Trabaja en una sucursal y solo opera guías en ella. */
    EMPLEADO,

    /** Opera guías en cualquier sucursal y da de alta empleados. */
    GERENTE
}

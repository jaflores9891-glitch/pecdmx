package mx.pecdmx.backend.dominio.usuario;

import java.util.Objects;
import java.util.Optional;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Quién está haciendo una operación: su correo, su rol y (si es empleado) su sucursal.
 * Se arma con los datos del gafete, sin consultar la base de datos.
 */
public record Operador(Correo correo, Rol rol, Sucursal sucursal) {

    public Operador {
        Objects.requireNonNull(correo, "El correo del operador es obligatorio");
        Objects.requireNonNull(rol, "El rol del operador es obligatorio");
        if (rol == Rol.EMPLEADO && sucursal == null) {
            throw new UsuarioInvalidoException("Un empleado debe pertenecer a una sucursal.");
        }
        if (rol == Rol.GERENTE && sucursal != null) {
            throw new UsuarioInvalidoException("Un gerente trabaja en todas las sucursales; no se le asigna una.");
        }
    }

    public boolean esGerente() {
        return rol == Rol.GERENTE;
    }

    /** La sucursal del empleado. Vacío para un gerente. */
    public Optional<Sucursal> sucursalAsignada() {
        return Optional.ofNullable(sucursal);
    }

    /** Un gerente opera en cualquier sucursal; un empleado, solo en la suya. */
    public boolean puedeOperarEn(Sucursal lugar) {
        Objects.requireNonNull(lugar, "La sucursal es obligatoria");
        return esGerente() || sucursal == lugar;
    }
}

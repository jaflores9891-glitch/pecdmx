package mx.pecdmx.backend.dominio.usuario;

import java.util.Objects;
import java.util.Optional;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Una persona con cuenta: empleado de una sucursal o gerente.
 * Guarda la contraseña ya cifrada (su "huella"); el dominio nunca ve la contraseña real.
 */
public record Usuario(Correo correo, String nombre, Rol rol, Sucursal sucursal, String contrasenaCifrada) {

    public static final int LONGITUD_MAXIMA_NOMBRE = 100;

    public Usuario {
        Objects.requireNonNull(correo, "El correo es obligatorio");
        Objects.requireNonNull(rol, "El rol es obligatorio");
        if (nombre == null || nombre.isBlank()) {
            throw new UsuarioInvalidoException("El nombre es obligatorio.");
        }
        nombre = nombre.strip();
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new UsuarioInvalidoException(
                    "El nombre no puede tener más de " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
        if (rol == Rol.EMPLEADO && sucursal == null) {
            throw new UsuarioInvalidoException("Un empleado debe pertenecer a una sucursal.");
        }
        if (rol == Rol.GERENTE && sucursal != null) {
            throw new UsuarioInvalidoException("Un gerente trabaja en todas las sucursales; no se le asigna una.");
        }
        if (contrasenaCifrada == null || contrasenaCifrada.isBlank()) {
            throw new UsuarioInvalidoException("La contraseña cifrada es obligatoria.");
        }
    }

    public static Usuario empleado(Correo correo, String nombre, Sucursal sucursal, String contrasenaCifrada) {
        return new Usuario(correo, nombre, Rol.EMPLEADO, sucursal, contrasenaCifrada);
    }

    public static Usuario gerente(Correo correo, String nombre, String contrasenaCifrada) {
        return new Usuario(correo, nombre, Rol.GERENTE, null, contrasenaCifrada);
    }

    /** La sucursal del empleado. Vacío para un gerente. */
    public Optional<Sucursal> sucursalAsignada() {
        return Optional.ofNullable(sucursal);
    }

    /** Un gerente opera en cualquier sucursal; un empleado, solo en la suya. */
    public boolean puedeOperarEn(Sucursal lugar) {
        Objects.requireNonNull(lugar, "La sucursal es obligatoria");
        return rol == Rol.GERENTE || sucursal == lugar;
    }

    /** Nunca muestra la contraseña cifrada (por si el usuario termina en un log). */
    @Override
    public String toString() {
        return "Usuario[" + correo + ", " + rol + (sucursal == null ? "" : ", " + sucursal) + "]";
    }
}

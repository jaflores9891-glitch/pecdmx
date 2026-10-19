package mx.pecdmx.backend.dominio.sucursal;

import java.util.Arrays;
import java.util.Locale;

/**
 * Las 6 sucursales de PECDMX. Es una lista cerrada: abrir una sucursal nueva es una decisión del negocio
 * que implica cambiar el código y sus tarifas.
 */
public enum Sucursal {

    CDMX("Ciudad de México", "Ciudad de México"),
    GDL("Guadalajara", "Jalisco"),
    MTY("Monterrey", "Nuevo León"),
    PUE("Puebla", "Puebla"),
    QRO("Querétaro", "Querétaro"),
    TIJ("Tijuana", "Baja California");

    private final String ciudad;
    private final String estado;

    Sucursal(String ciudad, String estado) {
        this.ciudad = ciudad;
        this.estado = estado;
    }

    public String codigo() {
        return name();
    }

    public String ciudad() {
        return ciudad;
    }

    public String estado() {
        return estado;
    }

    /**
     * Convierte el código que escribe un cliente ("mty", " Mty ") en una sucursal.
     *
     * @throws SucursalDesconocidaException si el código no corresponde a ninguna sucursal
     */
    public static Sucursal desdeCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new SucursalDesconocidaException(codigo);
        }
        String normalizado = codigo.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(sucursal -> sucursal.name().equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new SucursalDesconocidaException(codigo));
    }
}

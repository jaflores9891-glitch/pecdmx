package mx.pecdmx.backend.dominio.tarifa;

import java.math.BigDecimal;

/**
 * Zonas de tarifa según la distancia entre sucursales. Precios en pesos mexicanos, antes de IVA.
 * El primer kilo cuesta {@code precioBase}; cada kilo adicional cuesta {@code precioPorKgExtra}.
 */
public enum Zona {

    ZONA_1("Misma ciudad", "90.00", "10.00"),
    ZONA_2("Cercana (menos de 400 km)", "150.00", "18.00"),
    ZONA_3("Media (400 a 1,000 km)", "210.00", "25.00"),
    ZONA_4("Lejana (Tijuana)", "290.00", "35.00");

    private final String descripcion;
    private final BigDecimal precioBase;
    private final BigDecimal precioPorKgExtra;

    // Los precios se escriben como texto para que BigDecimal los guarde exactos (sin errores de redondeo).
    Zona(String descripcion, String precioBase, String precioPorKgExtra) {
        this.descripcion = descripcion;
        this.precioBase = new BigDecimal(precioBase);
        this.precioPorKgExtra = new BigDecimal(precioPorKgExtra);
    }

    public int numero() {
        return ordinal() + 1;
    }

    public String descripcion() {
        return descripcion;
    }

    public BigDecimal precioBase() {
        return precioBase;
    }

    public BigDecimal precioPorKgExtra() {
        return precioPorKgExtra;
    }
}

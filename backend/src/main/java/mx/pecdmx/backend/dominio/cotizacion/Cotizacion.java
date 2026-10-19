package mx.pecdmx.backend.dominio.cotizacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import mx.pecdmx.backend.dominio.tarifa.TablaDeZonas;
import mx.pecdmx.backend.dominio.tarifa.Zona;

/**
 * Precio de un envío con su desglose completo. Montos en pesos mexicanos, redondeados a centavos.
 * Se crea con {@link #calcular}, que aplica las reglas de tarifa de PECDMX.
 */
public record Cotizacion(
        Sucursal origen,
        Sucursal destino,
        Zona zona,
        BigDecimal pesoRealKg,
        BigDecimal pesoVolumetricoKg,
        int pesoCobrableKg,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total) {

    public static final BigDecimal TASA_IVA = new BigDecimal("0.16");

    /** subtotal = base de la zona + (kg cobrables − 1) × precio por kg extra; total = subtotal + 16% de IVA. */
    public static Cotizacion calcular(Sucursal origen, Sucursal destino, Paquete paquete) {
        Objects.requireNonNull(paquete, "El paquete es obligatorio");
        Zona zona = TablaDeZonas.zonaEntre(origen, destino);
        int kgCobrables = paquete.pesoCobrableKg();

        BigDecimal kgExtra = BigDecimal.valueOf(kgCobrables - 1L);
        BigDecimal subtotal = zona.precioBase()
                .add(zona.precioPorKgExtra().multiply(kgExtra))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);

        return new Cotizacion(
                origen,
                destino,
                zona,
                paquete.pesoKg(),
                paquete.pesoVolumetricoKg(),
                kgCobrables,
                subtotal,
                iva,
                subtotal.add(iva));
    }
}

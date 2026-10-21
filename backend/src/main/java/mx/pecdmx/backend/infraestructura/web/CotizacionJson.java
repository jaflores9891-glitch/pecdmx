package mx.pecdmx.backend.infraestructura.web;

import java.math.BigDecimal;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;

/** El JSON de respuesta de una cotización, con el desglose completo. Montos en pesos mexicanos. */
record CotizacionJson(
        SucursalJson origen,
        SucursalJson destino,
        int zona,
        String descripcionZona,
        BigDecimal pesoRealKg,
        BigDecimal pesoVolumetricoKg,
        int pesoCobrableKg,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        String moneda) {

    static CotizacionJson de(Cotizacion cotizacion) {
        return new CotizacionJson(
                SucursalJson.de(cotizacion.origen()),
                SucursalJson.de(cotizacion.destino()),
                cotizacion.zona().numero(),
                cotizacion.zona().descripcion(),
                cotizacion.pesoRealKg(),
                cotizacion.pesoVolumetricoKg(),
                cotizacion.pesoCobrableKg(),
                cotizacion.subtotal(),
                cotizacion.iva(),
                cotizacion.total(),
                "MXN");
    }
}

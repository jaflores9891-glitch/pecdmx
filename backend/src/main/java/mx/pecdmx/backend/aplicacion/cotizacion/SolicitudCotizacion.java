package mx.pecdmx.backend.aplicacion.cotizacion;

import java.math.BigDecimal;

/**
 * Datos sueltos que llegan de afuera para cotizar, tal como los escribe el cliente.
 * Todavía no están validados: eso lo hace el dominio al convertirlos.
 */
public record SolicitudCotizacion(
        String origen,
        String destino,
        BigDecimal pesoKg,
        int largoCm,
        int anchoCm,
        int altoCm) {
}

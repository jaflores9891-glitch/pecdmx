package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import mx.pecdmx.backend.aplicacion.cotizacion.SolicitudCotizacion;

/**
 * El JSON que llega para cotizar. Aquí solo se revisa que no falte nada;
 * las reglas del negocio (sucursal válida, máximo 70 kg...) las aplica el dominio.
 */
record SolicitudCotizacionJson(
        @NotBlank(message = "El origen es obligatorio") String origen,
        @NotBlank(message = "El destino es obligatorio") String destino,
        @NotNull(message = "El peso es obligatorio") BigDecimal pesoKg,
        @NotNull(message = "El largo es obligatorio") Integer largoCm,
        @NotNull(message = "El ancho es obligatorio") Integer anchoCm,
        @NotNull(message = "El alto es obligatorio") Integer altoCm) {

    SolicitudCotizacion aSolicitud() {
        return new SolicitudCotizacion(origen, destino, pesoKg, largoCm, anchoCm, altoCm);
    }
}

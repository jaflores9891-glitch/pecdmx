package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import mx.pecdmx.backend.aplicacion.cotizacion.SolicitudCotizacion;
import mx.pecdmx.backend.aplicacion.guia.SolicitudRegistroDeGuia;

/** El JSON para registrar una guía: los datos de cotizar más el nombre del destinatario. */
record SolicitudRegistroDeGuiaJson(
        @NotBlank(message = "El origen es obligatorio") String origen,
        @NotBlank(message = "El destino es obligatorio") String destino,
        @NotNull(message = "El peso es obligatorio") BigDecimal pesoKg,
        @NotNull(message = "El largo es obligatorio") Integer largoCm,
        @NotNull(message = "El ancho es obligatorio") Integer anchoCm,
        @NotNull(message = "El alto es obligatorio") Integer altoCm,
        // También se valida aquí (y no solo en el dominio) para que un nombre inválido sea un 400 claro.
        @NotBlank(message = "El nombre del destinatario es obligatorio")
        @Size(max = 100, message = "El nombre del destinatario no puede tener más de 100 caracteres")
        String nombreDestinatario) {

    SolicitudRegistroDeGuia aSolicitud() {
        return new SolicitudRegistroDeGuia(
                new SolicitudCotizacion(origen, destino, pesoKg, largoCm, anchoCm, altoCm), nombreDestinatario);
    }
}

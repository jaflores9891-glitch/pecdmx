package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.pecdmx.backend.aplicacion.guia.SolicitudCambioDeEstado;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * El JSON para cambiar el estado de una guía, por ejemplo {"estado": "EN_TRANSITO", "nota": "Salió en ruta"}.
 * "sucursal" solo la manda un gerente (un empleado opera siempre en la suya).
 */
record SolicitudCambioDeEstadoJson(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "REGISTRADA|EN_TRANSITO|EN_SUCURSAL_DESTINO|INCIDENCIA|ENTREGADA|CANCELADA",
                message = "El estado debe ser uno de: EN_TRANSITO, EN_SUCURSAL_DESTINO, INCIDENCIA, ENTREGADA, CANCELADA")
        String estado,
        @Size(max = 300, message = "La nota no puede tener más de 300 caracteres") String nota,
        String sucursal) {

    SolicitudCambioDeEstado aSolicitud(String numero) {
        return new SolicitudCambioDeEstado(
                numero,
                EstadoEnvio.valueOf(estado),
                nota,
                sucursal == null || sucursal.isBlank() ? null : Sucursal.desdeCodigo(sucursal));
    }
}

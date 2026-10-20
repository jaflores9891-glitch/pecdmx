package mx.pecdmx.backend.dominio.guia;

import java.time.Instant;
import java.util.Objects;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Una línea del historial de una guía: a qué estado pasó, cuándo, en qué sucursal y una nota opcional.
 * Ejemplo: "En tránsito · 2026-10-19 10:42 · CDMX · Salió en ruta a Monterrey".
 */
public record EventoDeRastreo(EstadoEnvio estado, Instant fechaHora, Sucursal sucursal, String nota) {

    public static final int LONGITUD_MAXIMA_NOTA = 300;

    public EventoDeRastreo {
        Objects.requireNonNull(estado, "El estado del evento es obligatorio");
        Objects.requireNonNull(fechaHora, "La fecha y hora del evento son obligatorias");
        Objects.requireNonNull(sucursal, "La sucursal del evento es obligatoria");
        nota = nota == null ? "" : nota.strip();
        if (nota.length() > LONGITUD_MAXIMA_NOTA) {
            throw new GuiaInvalidaException(
                    "La nota del evento no puede tener más de " + LONGITUD_MAXIMA_NOTA + " caracteres.");
        }
    }
}

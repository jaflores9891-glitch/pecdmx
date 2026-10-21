package mx.pecdmx.backend.aplicacion.guia;

import java.util.List;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.EventoDeRastreo;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Lo que cualquier persona con el número puede ver de una guía.
 * A propósito NO incluye el nombre del destinatario ni los montos cobrados.
 */
public record Rastreo(
        NumeroDeGuia numero,
        Sucursal origen,
        Sucursal destino,
        EstadoEnvio estadoActual,
        List<EventoDeRastreo> historial) {

    public Rastreo {
        historial = List.copyOf(historial);
    }

    static Rastreo de(Guia guia) {
        return new Rastreo(guia.numero(), guia.origen(), guia.destino(), guia.estadoActual(), guia.historial());
    }
}

package mx.pecdmx.backend.infraestructura.web;

import java.util.List;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.infraestructura.web.RastreoJson.EstadoJson;
import mx.pecdmx.backend.infraestructura.web.RastreoJson.EventoJson;

/** El JSON completo de una guía, solo para personal autorizado: incluye destinatario y montos. */
record DetalleDeGuiaJson(
        String numero,
        String nombreDestinatario,
        CotizacionJson cotizacion,
        EstadoJson estado,
        List<EventoJson> historial) {

    static DetalleDeGuiaJson de(Guia guia) {
        return new DetalleDeGuiaJson(
                guia.numero().valor(),
                guia.nombreDestinatario(),
                CotizacionJson.de(guia.cotizacion()),
                EstadoJson.de(guia.estadoActual()),
                guia.historial().stream().map(EventoJson::de).toList());
    }
}

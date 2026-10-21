package mx.pecdmx.backend.infraestructura.web;

import java.time.Instant;
import java.util.List;
import mx.pecdmx.backend.aplicacion.guia.Rastreo;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.EventoDeRastreo;

/** El JSON público del rastreo de una guía. Sale de {@link Rastreo}, que ya no trae datos personales. */
record RastreoJson(
        String numero,
        SucursalJson origen,
        SucursalJson destino,
        EstadoJson estado,
        List<EventoJson> historial) {

    /** Un estado con su código (para programas) y sus textos (para personas). */
    record EstadoJson(String codigo, String nombre, String descripcion) {

        static EstadoJson de(EstadoEnvio estado) {
            return new EstadoJson(estado.name(), estado.nombre(), estado.descripcion());
        }
    }

    /** Una línea del historial. La hora va en formato ISO-8601 y en UTC (ej. "2026-10-20T15:00:00Z"). */
    record EventoJson(EstadoJson estado, Instant fechaHora, SucursalJson sucursal, String nota) {

        static EventoJson de(EventoDeRastreo evento) {
            return new EventoJson(
                    EstadoJson.de(evento.estado()), evento.fechaHora(), SucursalJson.de(evento.sucursal()), evento.nota());
        }
    }

    static RastreoJson de(Rastreo rastreo) {
        return new RastreoJson(
                rastreo.numero().valor(),
                SucursalJson.de(rastreo.origen()),
                SucursalJson.de(rastreo.destino()),
                EstadoJson.de(rastreo.estadoActual()),
                rastreo.historial().stream().map(EventoJson::de).toList());
    }
}

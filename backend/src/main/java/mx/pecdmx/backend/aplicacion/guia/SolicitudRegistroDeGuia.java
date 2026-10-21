package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.aplicacion.cotizacion.SolicitudCotizacion;

/** Datos para registrar una guía: los mismos de una cotización, más el nombre de quien la recoge. */
public record SolicitudRegistroDeGuia(SolicitudCotizacion envio, String nombreDestinatario) {
}

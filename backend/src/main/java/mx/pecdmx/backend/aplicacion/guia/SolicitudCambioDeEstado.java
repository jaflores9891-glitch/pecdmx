package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Datos para avanzar una guía a un estado nuevo.
 *
 * @param sucursal dónde ocurre el evento. Un empleado puede omitirla (se usa la suya); un gerente debe indicarla.
 */
public record SolicitudCambioDeEstado(String numero, EstadoEnvio estado, String nota, Sucursal sucursal) {
}

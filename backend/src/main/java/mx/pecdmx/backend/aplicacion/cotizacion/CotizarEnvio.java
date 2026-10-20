package mx.pecdmx.backend.aplicacion.cotizacion;

import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;

/**
 * Puerto de entrada: lo que el mundo exterior (API web, chatbot) puede pedirle al negocio para cotizar.
 * Quien lo use depende de este contrato, no de cómo se implementa.
 */
public interface CotizarEnvio {

    /**
     * @throws mx.pecdmx.backend.dominio.sucursal.SucursalDesconocidaException si origen o destino no existen
     * @throws mx.pecdmx.backend.dominio.tarifa.PaqueteInvalidoException si el peso o las medidas no son válidos
     */
    Cotizacion cotizar(SolicitudCotizacion solicitud);
}

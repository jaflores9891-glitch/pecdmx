package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.usuario.Operador;

/** Puerto de entrada: ver una guía completa, con destinatario y montos (solo personal autorizado). */
public interface ConsultarDetalleDeGuia {

    /**
     * @throws mx.pecdmx.backend.dominio.guia.NumeroDeGuiaInvalidoException si el número está mal escrito
     * @throws GuiaNoEncontradaException si la guía no existe
     * @throws OperacionNoPermitidaException si es un empleado de una sucursal que no es origen ni destino
     */
    Guia consultar(String numero, Operador operador);
}

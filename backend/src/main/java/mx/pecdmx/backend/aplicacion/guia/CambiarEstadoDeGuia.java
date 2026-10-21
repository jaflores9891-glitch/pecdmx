package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.usuario.Operador;

/** Puerto de entrada: un empleado o gerente anota que la guía avanzó (salió, llegó, se entregó...). */
public interface CambiarEstadoDeGuia {

    /**
     * @throws SolicitudInvalidaException si un gerente no indica la sucursal
     * @throws OperacionNoPermitidaException si un empleado indica una sucursal que no es la suya
     * @throws GuiaNoEncontradaException si la guía no existe
     * @throws CambioDeEstadoRechazadoException si el cambio choca con el estado actual de la guía
     * @throws GuiaDesactualizadaException si alguien más la modificó al mismo tiempo
     */
    Guia cambiar(SolicitudCambioDeEstado solicitud, Operador operador);
}

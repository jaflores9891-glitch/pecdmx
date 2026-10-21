package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.usuario.Operador;

/** Puerto de entrada: un empleado o gerente registra un paquete que recibió en sucursal. */
public interface RegistrarGuia {

    /**
     * @throws OperacionNoPermitidaException si un empleado registra con origen en otra sucursal
     * @throws mx.pecdmx.backend.dominio.sucursal.SucursalDesconocidaException si origen o destino no existen
     * @throws mx.pecdmx.backend.dominio.tarifa.PaqueteInvalidoException si el paquete no es válido
     * @throws mx.pecdmx.backend.dominio.guia.GuiaInvalidaException si falta el nombre del destinatario
     */
    Guia registrar(SolicitudRegistroDeGuia solicitud, Operador operador);
}

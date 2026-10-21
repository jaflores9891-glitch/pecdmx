package mx.pecdmx.backend.aplicacion.guia;

import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;

/** Se lanza cuando un número de guía está bien escrito pero no corresponde a ninguna guía registrada. */
public class GuiaNoEncontradaException extends RuntimeException {

    public GuiaNoEncontradaException(NumeroDeGuia numero) {
        super("No encontramos la guía " + numero + ". Revisa el número o pregunta en tu sucursal.");
    }
}

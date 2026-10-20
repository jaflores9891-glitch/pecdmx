package mx.pecdmx.backend.dominio.guia;

import java.util.stream.Collectors;

/** Se lanza cuando se intenta un cambio de estado que las reglas no permiten. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoEnvio actual, EstadoEnvio destino) {
        super(mensaje(actual, destino));
    }

    private static String mensaje(EstadoEnvio actual, EstadoEnvio destino) {
        String base = "Una guía en estado '" + actual.nombre() + "' no puede pasar a '" + destino.nombre() + "'.";
        if (actual.esFinal()) {
            return base + " '" + actual.nombre() + "' es un estado final.";
        }
        String posibles = actual.siguientesPosibles().stream()
                .map(EstadoEnvio::nombre)
                .collect(Collectors.joining(", "));
        return base + " Estados posibles: " + posibles + ".";
    }
}

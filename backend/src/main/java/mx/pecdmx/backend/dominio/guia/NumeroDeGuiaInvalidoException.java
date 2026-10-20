package mx.pecdmx.backend.dominio.guia;

/** Se lanza cuando un número de guía no tiene el formato correcto o su dígito verificador no coincide. */
public class NumeroDeGuiaInvalidoException extends RuntimeException {

    public NumeroDeGuiaInvalidoException(String valor) {
        super("El número de guía '" + valor + "' no es válido. Revisa que esté bien escrito: "
                + "son las letras PEC seguidas de 10 dígitos, por ejemplo PEC2610190049.");
    }
}

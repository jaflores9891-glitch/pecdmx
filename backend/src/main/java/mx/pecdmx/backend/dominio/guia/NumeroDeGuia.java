package mx.pecdmx.backend.dominio.guia;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Número de guía de PECDMX: "PEC" + 9 dígitos + 1 dígito verificador (algoritmo de Luhn).
 * El verificador detecta casi cualquier error al escribir un dígito o al voltear dos dígitos seguidos,
 * así que una guía mal escrita se rechaza sin consultar la base de datos.
 */
public record NumeroDeGuia(String valor) {

    public static final String PREFIJO = "PEC";
    private static final Pattern FORMATO = Pattern.compile("PEC\\d{10}");
    private static final long SECUENCIA_MAXIMA = 999_999_999L;

    public NumeroDeGuia {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new NumeroDeGuiaInvalidoException(valor);
        }
        String digitos = valor.substring(PREFIJO.length());
        char verificador = digitos.charAt(9);
        if (digitoVerificador(digitos.substring(0, 9)) != verificador) {
            throw new NumeroDeGuiaInvalidoException(valor);
        }
    }

    /** Lee una guía tal como la escribe el cliente: acepta minúsculas, espacios y guiones ("pec 261019004-9"). */
    public static NumeroDeGuia desdeTexto(String texto) {
        if (texto == null) {
            throw new NumeroDeGuiaInvalidoException(null);
        }
        String limpio = texto.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return new NumeroDeGuia(limpio);
    }

    /** Crea la guía número {@code secuencia} (1, 2, 3...) con su dígito verificador. */
    public static NumeroDeGuia deSecuencia(long secuencia) {
        if (secuencia < 1 || secuencia > SECUENCIA_MAXIMA) {
            throw new IllegalArgumentException("La secuencia debe estar entre 1 y " + SECUENCIA_MAXIMA);
        }
        String base = String.format("%09d", secuencia);
        return new NumeroDeGuia(PREFIJO + base + digitoVerificador(base));
    }

    /** Algoritmo de Luhn: de derecha a izquierda, se duplica un dígito sí y otro no. */
    static char digitoVerificador(String nueveDigitos) {
        int suma = 0;
        boolean duplicar = true;
        for (int i = nueveDigitos.length() - 1; i >= 0; i--) {
            int digito = nueveDigitos.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return (char) ('0' + (10 - suma % 10) % 10);
    }

    @Override
    public String toString() {
        return valor;
    }
}

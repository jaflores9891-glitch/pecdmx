package mx.pecdmx.backend.dominio.usuario;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electrónico de un usuario, siempre en minúsculas y con formato básico válido.
 * Así "Ana@PECDMX.mx" y "ana@pecdmx.mx" son la misma cuenta.
 */
public record Correo(String valor) {

    public static final int LONGITUD_MAXIMA = 254;

    // Revisión sencilla a propósito: algo@algo.algo, sin espacios. La única forma real de validar un correo es escribirle.
    private static final Pattern FORMATO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new UsuarioInvalidoException("El correo es obligatorio.");
        }
        valor = valor.strip().toLowerCase(Locale.ROOT);
        if (valor.length() > LONGITUD_MAXIMA || !FORMATO.matcher(valor).matches()) {
            throw new UsuarioInvalidoException("El correo '" + valor + "' no tiene un formato válido.");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}

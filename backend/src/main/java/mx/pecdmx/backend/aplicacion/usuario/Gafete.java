package mx.pecdmx.backend.aplicacion.usuario;

import java.time.Instant;

/** El "gafete digital" que recibe un usuario al iniciar sesión y presenta en cada petición. */
public record Gafete(String valor, Instant expiraEn) {

    /** Nunca imprime el gafete completo: quien lo tenga puede hacerse pasar por el usuario. */
    @Override
    public String toString() {
        return "Gafete[expiraEn=" + expiraEn + "]";
    }
}

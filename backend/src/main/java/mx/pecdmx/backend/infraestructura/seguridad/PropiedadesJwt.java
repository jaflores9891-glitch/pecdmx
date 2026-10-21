package mx.pecdmx.backend.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los gafetes JWT, leída de la variable de entorno PECDMX_JWT_SECRETO.
 * Si el secreto falta o es corto, el servicio NO arranca: así nunca queda en producción con una llave débil.
 */
@ConfigurationProperties(prefix = "pecdmx.jwt")
record PropiedadesJwt(String secreto, Duration duracion) {

    /** HMAC-SHA256 necesita una llave de al menos 256 bits (32 bytes). */
    static final int BYTES_MINIMOS_SECRETO = 32;

    PropiedadesJwt {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("Falta la variable PECDMX_JWT_SECRETO o tiene menos de "
                    + BYTES_MINIMOS_SECRETO + " caracteres. Configúrala en tu .env o en el panel de Render.");
        }
        if (duracion == null || duracion.isNegative() || duracion.isZero()) {
            throw new IllegalStateException("pecdmx.jwt.duracion debe ser mayor a cero.");
        }
    }

    /** Nunca imprime el secreto. */
    @Override
    public String toString() {
        return "PropiedadesJwt[duracion=" + duracion + "]";
    }
}

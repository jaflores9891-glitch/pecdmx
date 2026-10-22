package mx.pecdmx.backend.infraestructura.seguridad;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * La "lista de invitados": qué páginas web pueden hablar con el backend desde el navegador.
 * Se lee de PECDMX_CORS_ORIGENES, separadas por comas, por ejemplo "https://pecdmx.vercel.app,http://localhost:5173".
 */
@ConfigurationProperties(prefix = "pecdmx.cors")
record PropiedadesCors(List<String> origenes) {

    /** http(s)://dominio[:puerto], sin ruta ni "/" final: así es como el navegador manda su origen. */
    private static final Pattern ORIGEN = Pattern.compile("^https?://[a-zA-Z0-9.-]+(:\\d+)?$");

    PropiedadesCors {
        origenes = origenes == null ? List.of() : origenes.stream().map(String::strip).filter(o -> !o.isEmpty()).toList();
        for (String origen : origenes) {
            if (origen.equals("*")) {
                throw new IllegalStateException(
                        "PECDMX_CORS_ORIGENES no puede ser \"*\": eso dejaría a cualquier página usar el backend.");
            }
            if (!ORIGEN.matcher(origen).matches()) {
                throw new IllegalStateException("El origen '" + origen + "' de PECDMX_CORS_ORIGENES no es válido. "
                        + "Debe ser como https://pecdmx.vercel.app (con http:// o https:// y sin / al final).");
            }
        }
    }
}

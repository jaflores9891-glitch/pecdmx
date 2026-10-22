package mx.pecdmx.backend.infraestructura.seguridad;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Qué se les permite a las páginas de la lista de invitados (CORS).
 * Solo lo que usa la web: GET y POST, y los encabezados para mandar JSON y el gafete.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesCors.class)
class CorsConfig {

    @Bean
    CorsConfigurationSource reglasCors(PropiedadesCors propiedades) {
        CorsConfiguration reglas = new CorsConfiguration();
        reglas.setAllowedOrigins(propiedades.origenes());
        reglas.setAllowedMethods(List.of("GET", "POST"));
        reglas.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        // El gafete viaja en el encabezado Authorization, no en cookies: no hace falta permitir credenciales.
        reglas.setAllowCredentials(false);
        // El navegador recuerda el permiso 1 hora y no vuelve a preguntar en cada petición.
        reglas.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", reglas);
        return fuente;
    }
}

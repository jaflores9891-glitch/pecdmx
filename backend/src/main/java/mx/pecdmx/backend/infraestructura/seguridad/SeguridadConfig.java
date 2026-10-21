package mx.pecdmx.backend.infraestructura.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import mx.pecdmx.backend.aplicacion.usuario.EmisorDeGafetes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Las reglas del "guardia": qué es público, qué pide gafete y cómo se firman y revisan los gafetes. */
@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
class SeguridadConfig {

    @Bean
    SecurityFilterChain reglas(HttpSecurity http) throws Exception {
        http
                // Sin cookies de sesión: cada petición trae su gafete. Por eso no hace falta la protección CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        // Lo público: lo que usan los clientes sin cuenta.
                        .requestMatchers(HttpMethod.GET, "/api/sucursales", "/api/guias/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cotizaciones", "/api/sesiones").permitAll()
                        // Para que Render sepa si el servicio está vivo.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Donde Spring responde los errores; si no fuera público, todo error se vería como 401.
                        .requestMatchers("/error").permitAll()
                        // Operar guías: personal con cuenta. (El rastreo público ya quedó permitido arriba.)
                        .requestMatchers("/api/guias", "/api/guias/**").hasAnyRole("EMPLEADO", "GERENTE")
                        // Cuentas de empleados: solo gerentes (el caso de uso también lo revisa).
                        .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("GERENTE")
                        // Todo lo demás pide gafete.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDelGafete())));
        return http.build();
    }

    /** Convierte el reclamo "rol" del gafete (ej. "GERENTE") en el permiso "ROLE_GERENTE" que entiende Spring. */
    private static JwtAuthenticationConverter rolesDelGafete() {
        JwtGrantedAuthoritiesConverter permisos = new JwtGrantedAuthoritiesConverter();
        permisos.setAuthoritiesClaimName(EmisorDeGafetesJwt.RECLAMO_ROL);
        permisos.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(permisos);
        return convertidor;
    }

    @Bean
    JwtEncoder codificadorJwt(PropiedadesJwt propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(llave(propiedades)));
    }

    /** Revisa la firma, que no haya expirado y que lo haya emitido PECDMX. */
    @Bean
    JwtDecoder decodificadorJwt(PropiedadesJwt propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(llave(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EmisorDeGafetesJwt.EMISOR));
        return decodificador;
    }

    @Bean
    EmisorDeGafetes emisorDeGafetes(JwtEncoder codificador, PropiedadesJwt propiedades) {
        return new EmisorDeGafetesJwt(codificador, propiedades);
    }

    private static SecretKey llave(PropiedadesJwt propiedades) {
        return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}

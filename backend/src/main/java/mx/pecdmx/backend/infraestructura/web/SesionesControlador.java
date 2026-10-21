package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import mx.pecdmx.backend.aplicacion.usuario.Gafete;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeSesiones;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/sesiones: iniciar sesión. GET /api/sesiones/yo: quién soy, según mi gafete. */
@RestController
@RequestMapping("/api/sesiones")
class SesionesControlador {

    record SolicitudSesionJson(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String contrasena) {

        @Override
        public String toString() {
            return "SolicitudSesionJson[correo=" + correo + "]";
        }
    }

    /** "tipo": "Bearer" indica cómo mandar el gafete: en el encabezado {@code Authorization: Bearer <gafete>}. */
    record SesionJson(String gafete, String tipo, Instant expiraEn) {
    }

    record UsuarioActualJson(String correo, String nombre, String rol, String sucursal) {
    }

    private final ServicioDeSesiones servicioDeSesiones;

    SesionesControlador(ServicioDeSesiones servicioDeSesiones) {
        this.servicioDeSesiones = servicioDeSesiones;
    }

    @PostMapping
    SesionJson iniciar(@Valid @RequestBody SolicitudSesionJson solicitud) {
        Gafete gafete = servicioDeSesiones.iniciarSesion(solicitud.correo(), solicitud.contrasena());
        return new SesionJson(gafete.valor(), "Bearer", gafete.expiraEn());
    }

    // Spring Security ya revisó la firma del gafete; aquí solo se leen sus datos.
    @GetMapping("/yo")
    UsuarioActualJson yo(@AuthenticationPrincipal Jwt gafete) {
        return new UsuarioActualJson(
                gafete.getSubject(),
                gafete.getClaimAsString("nombre"),
                gafete.getClaimAsString("rol"),
                gafete.getClaimAsString("sucursal"));
    }
}

package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import mx.pecdmx.backend.aplicacion.usuario.DarDeAltaEmpleado;
import mx.pecdmx.backend.aplicacion.usuario.SolicitudAltaDeEmpleado;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/usuarios: un gerente da de alta a un empleado. */
@RestController
@RequestMapping("/api/usuarios")
class UsuariosControlador {

    record SolicitudAltaJson(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "La sucursal es obligatoria") String sucursal,
            @NotBlank(message = "La contraseña inicial es obligatoria") String contrasenaInicial) {

        SolicitudAltaDeEmpleado aSolicitud() {
            return new SolicitudAltaDeEmpleado(correo, nombre, Sucursal.desdeCodigo(sucursal), contrasenaInicial);
        }

        @Override
        public String toString() {
            return "SolicitudAltaJson[correo=" + correo + ", nombre=" + nombre + ", sucursal=" + sucursal + "]";
        }
    }

    /** Lo que se responde de un usuario. A propósito no tiene ningún campo de contraseña. */
    record UsuarioJson(String correo, String nombre, String rol, String sucursal) {

        static UsuarioJson de(Usuario usuario) {
            return new UsuarioJson(
                    usuario.correo().valor(),
                    usuario.nombre(),
                    usuario.rol().name(),
                    usuario.sucursalAsignada().map(Sucursal::codigo).orElse(null));
        }
    }

    private final DarDeAltaEmpleado darDeAlta;

    UsuariosControlador(DarDeAltaEmpleado darDeAlta) {
        this.darDeAlta = darDeAlta;
    }

    @PostMapping
    ResponseEntity<UsuarioJson> darDeAlta(
            @Valid @RequestBody SolicitudAltaJson solicitud, @AuthenticationPrincipal Jwt gafete) {
        Usuario empleado = darDeAlta.darDeAlta(solicitud.aSolicitud(), OperadorDelGafete.de(gafete));
        return ResponseEntity.created(URI.create("/api/usuarios/" + empleado.correo().valor()))
                .body(UsuarioJson.de(empleado));
    }
}

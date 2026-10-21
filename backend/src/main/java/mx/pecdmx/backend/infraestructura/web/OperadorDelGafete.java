package mx.pecdmx.backend.infraestructura.web;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Operador;
import mx.pecdmx.backend.dominio.usuario.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

/** Traduce un gafete JWT (ya verificado por Spring Security) al {@link Operador} del dominio. */
final class OperadorDelGafete {

    private OperadorDelGafete() {
    }

    static Operador de(Jwt gafete) {
        String sucursal = gafete.getClaimAsString("sucursal");
        return new Operador(
                new Correo(gafete.getSubject()),
                Rol.valueOf(gafete.getClaimAsString("rol")),
                sucursal == null ? null : Sucursal.desdeCodigo(sucursal));
    }
}

package mx.pecdmx.backend.infraestructura.seguridad;

import java.time.Instant;
import mx.pecdmx.backend.aplicacion.usuario.EmisorDeGafetes;
import mx.pecdmx.backend.aplicacion.usuario.Gafete;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Adaptador: los gafetes son JWT firmados con HMAC-SHA256.
 * Llevan quién es el usuario ("sub"), su rol y su sucursal, para no consultar la base de datos en cada petición.
 */
class EmisorDeGafetesJwt implements EmisorDeGafetes {

    static final String EMISOR = "pecdmx";
    static final String RECLAMO_ROL = "rol";
    static final String RECLAMO_SUCURSAL = "sucursal";
    static final String RECLAMO_NOMBRE = "nombre";

    private final JwtEncoder codificador;
    private final PropiedadesJwt propiedades;

    EmisorDeGafetesJwt(JwtEncoder codificador, PropiedadesJwt propiedades) {
        this.codificador = codificador;
        this.propiedades = propiedades;
    }

    @Override
    public Gafete emitir(Usuario usuario, Instant ahora) {
        Instant expiraEn = ahora.plus(propiedades.duracion());
        JwtClaimsSet.Builder reclamos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.correo().valor())
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .claim(RECLAMO_NOMBRE, usuario.nombre())
                .claim(RECLAMO_ROL, usuario.rol().name());
        usuario.sucursalAsignada().ifPresent(sucursal -> reclamos.claim(RECLAMO_SUCURSAL, sucursal.codigo()));

        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(encabezado, reclamos.build())).getTokenValue();
        return new Gafete(valor, expiraEn);
    }
}

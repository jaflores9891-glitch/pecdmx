package mx.pecdmx.backend.infraestructura.seguridad;

import mx.pecdmx.backend.aplicacion.usuario.CifradorDeContrasenas;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador: cifra contraseñas con BCrypt. Cada huella lleva su propia "sal" (un valor al azar),
 * así que dos personas con la misma contraseña tienen huellas distintas.
 */
@Component
class CifradorBCrypt implements CifradorDeContrasenas {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String cifrar(String contrasena) {
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String contrasenaCifrada) {
        return bcrypt.matches(contrasena, contrasenaCifrada);
    }
}

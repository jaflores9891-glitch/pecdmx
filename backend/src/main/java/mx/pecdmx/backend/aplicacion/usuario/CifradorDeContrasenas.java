package mx.pecdmx.backend.aplicacion.usuario;

/**
 * Puerto de salida: convertir una contraseña en su "huella" y comparar.
 * La aplicación no sabe qué algoritmo se usa (BCrypt); eso lo decide un adaptador.
 */
public interface CifradorDeContrasenas {

    String cifrar(String contrasena);

    boolean coincide(String contrasena, String contrasenaCifrada);
}

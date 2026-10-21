package mx.pecdmx.backend.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del primer gerente, leídos de las variables de entorno PECDMX_GERENTE_* (nunca del código).
 * Ver application.properties y .env.example.
 */
@ConfigurationProperties(prefix = "pecdmx.gerente-inicial")
record PropiedadesGerenteInicial(String correo, String nombre, String contrasena) {

    boolean estanCompletas() {
        return noVacio(correo) && noVacio(nombre) && noVacio(contrasena);
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Nunca imprime la contraseña. */
    @Override
    public String toString() {
        return "PropiedadesGerenteInicial[correo=" + correo + ", nombre=" + nombre + "]";
    }
}

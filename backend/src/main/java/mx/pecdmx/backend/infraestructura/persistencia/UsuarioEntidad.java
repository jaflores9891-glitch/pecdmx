package mx.pecdmx.backend.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Rol;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/** Una fila de la tabla {@code usuarios}. Solo la usa el adaptador de persistencia. */
@Entity
@Table(name = "usuarios")
class UsuarioEntidad {

    @Id
    private String correo;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    private Sucursal sucursal;

    @Column(name = "contrasena_cifrada", nullable = false)
    private String contrasenaCifrada;

    protected UsuarioEntidad() {
    }

    static UsuarioEntidad desde(Usuario usuario) {
        UsuarioEntidad entidad = new UsuarioEntidad();
        entidad.correo = usuario.correo().valor();
        entidad.nombre = usuario.nombre();
        entidad.rol = usuario.rol();
        entidad.sucursal = usuario.sucursal();
        entidad.contrasenaCifrada = usuario.contrasenaCifrada();
        return entidad;
    }

    Usuario aDominio() {
        return new Usuario(new Correo(correo), nombre, rol, sucursal, contrasenaCifrada);
    }
}

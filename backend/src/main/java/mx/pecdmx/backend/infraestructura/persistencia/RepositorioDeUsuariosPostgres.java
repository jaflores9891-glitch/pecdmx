package mx.pecdmx.backend.infraestructura.persistencia;

import java.util.Optional;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador: cumple el puerto {@link RepositorioDeUsuarios} guardando los usuarios en PostgreSQL. */
@Repository
class RepositorioDeUsuariosPostgres implements RepositorioDeUsuarios {

    private final UsuarioRepositorioJpa jpa;

    RepositorioDeUsuariosPostgres(UsuarioRepositorioJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(Correo correo) {
        return jpa.findById(correo.valor()).map(UsuarioEntidad::aDominio);
    }

    @Override
    @Transactional
    public void guardar(Usuario usuario) {
        jpa.save(UsuarioEntidad.desde(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hayUsuarios() {
        return jpa.count() > 0;
    }
}

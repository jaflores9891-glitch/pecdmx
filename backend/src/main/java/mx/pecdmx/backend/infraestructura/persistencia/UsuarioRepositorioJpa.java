package mx.pecdmx.backend.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioRepositorioJpa extends JpaRepository<UsuarioEntidad, String> {
}

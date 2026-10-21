package mx.pecdmx.backend.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data genera el SQL de estos métodos (guardar, buscar por id...) a partir de la interfaz. */
interface GuiaRepositorioJpa extends JpaRepository<GuiaEntidad, String> {

    @Query(value = "SELECT nextval('guia_secuencia')", nativeQuery = true)
    long siguienteSecuencia();
}

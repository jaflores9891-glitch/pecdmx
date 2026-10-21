package mx.pecdmx.backend.infraestructura.persistencia;

import java.util.List;
import java.util.Optional;
import mx.pecdmx.backend.aplicacion.guia.GuiaDesactualizadaException;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.dominio.guia.EventoDeRastreo;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador: cumple el puerto {@link RepositorioDeGuias} guardando las guías en PostgreSQL con JPA. */
@Repository
class RepositorioDeGuiasPostgres implements RepositorioDeGuias {

    private final GuiaRepositorioJpa jpa;

    RepositorioDeGuiasPostgres(GuiaRepositorioJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public NumeroDeGuia siguienteNumero() {
        return NumeroDeGuia.deSecuencia(jpa.siguienteSecuencia());
    }

    /**
     * Una guía nueva se inserta completa. De una existente solo se agregan los eventos nuevos,
     * y antes se revisa que lo ya guardado coincida con lo que trae la guía (nadie más la cambió).
     */
    @Override
    @Transactional
    public void guardar(Guia guia) {
        Optional<GuiaEntidad> existente = jpa.findById(guia.numero().valor());
        if (existente.isEmpty()) {
            jpa.save(GuiaEntidad.desde(guia));
            return;
        }

        GuiaEntidad entidad = existente.get();
        List<EventoDeRastreo> guardados = entidad.eventosDominio();
        List<EventoDeRastreo> actuales = guia.historial();
        boolean mismoInicio = actuales.size() >= guardados.size()
                && actuales.subList(0, guardados.size()).equals(guardados);
        if (!mismoInicio) {
            throw new GuiaDesactualizadaException(guia.numero());
        }
        actuales.subList(guardados.size(), actuales.size()).forEach(entidad::agregarEvento);
    }

    // La transacción mantiene abierta la conexión mientras se leen los eventos de la guía.
    @Override
    @Transactional(readOnly = true)
    public Optional<Guia> buscarPorNumero(NumeroDeGuia numero) {
        return jpa.findById(numero.valor()).map(GuiaEntidad::aDominio);
    }
}

package mx.pecdmx.backend.aplicacion.guia;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;

/**
 * Repositorio falso, solo para tests: guarda las guías en un mapa en memoria.
 * Permite probar los casos de uso en milisegundos, sin Docker ni PostgreSQL.
 */
public class RepositorioDeGuiasEnMemoria implements RepositorioDeGuias {

    private final Map<NumeroDeGuia, Guia> guias = new HashMap<>();
    private final AtomicLong secuencia = new AtomicLong();
    private int busquedas;

    @Override
    public NumeroDeGuia siguienteNumero() {
        return NumeroDeGuia.deSecuencia(secuencia.incrementAndGet());
    }

    @Override
    public void guardar(Guia guia) {
        guias.put(guia.numero(), guia);
    }

    @Override
    public Optional<Guia> buscarPorNumero(NumeroDeGuia numero) {
        busquedas++;
        return Optional.ofNullable(guias.get(numero));
    }

    /** Cuántas veces se consultó el repositorio (para comprobar que no se consulta de más). */
    public int busquedas() {
        return busquedas;
    }
}

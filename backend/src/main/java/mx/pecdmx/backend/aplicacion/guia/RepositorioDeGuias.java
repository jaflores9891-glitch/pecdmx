package mx.pecdmx.backend.aplicacion.guia;

import java.util.Optional;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;

/**
 * Puerto de salida: lo que el negocio necesita para guardar y encontrar guías.
 * No dice cómo se hace (PostgreSQL, memoria, archivo); eso lo decide un adaptador en infraestructura.
 */
public interface RepositorioDeGuias {

    /** Da el siguiente número de guía libre. Nunca repite un número, aunque dos sucursales pidan uno al mismo tiempo. */
    NumeroDeGuia siguienteNumero();

    /** Guarda una guía nueva o los eventos nuevos de una guía existente. */
    void guardar(Guia guia);

    /** Busca una guía con todo su historial. Vacío si no existe. */
    Optional<Guia> buscarPorNumero(NumeroDeGuia numero);
}

package mx.pecdmx.backend.aplicacion.guia;

import java.util.Objects;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;

/** Atiende las consultas de rastreo: valida el número, busca la guía y devuelve solo la parte pública. */
public class ServicioDeRastreo implements RastrearGuia {

    private final RepositorioDeGuias repositorio;

    public ServicioDeRastreo(RepositorioDeGuias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override
    public Rastreo rastrear(String numero) {
        // Primero se valida el número: una guía mal escrita se rechaza sin consultar la base de datos.
        NumeroDeGuia numeroDeGuia = NumeroDeGuia.desdeTexto(numero);
        return repositorio.buscarPorNumero(numeroDeGuia)
                .map(Rastreo::de)
                .orElseThrow(() -> new GuiaNoEncontradaException(numeroDeGuia));
    }
}

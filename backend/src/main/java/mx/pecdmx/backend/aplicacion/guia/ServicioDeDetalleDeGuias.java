package mx.pecdmx.backend.aplicacion.guia;

import java.util.Objects;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.usuario.Operador;

/**
 * Muestra el detalle completo de una guía. Lo ve cualquier gerente, o un empleado de la sucursal
 * de origen o de destino: los datos personales no se comparten con sucursales que no participan en el envío.
 */
public class ServicioDeDetalleDeGuias implements ConsultarDetalleDeGuia {

    private final RepositorioDeGuias repositorio;

    public ServicioDeDetalleDeGuias(RepositorioDeGuias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override
    public Guia consultar(String numero, Operador operador) {
        Objects.requireNonNull(operador, "El operador es obligatorio");
        NumeroDeGuia numeroDeGuia = NumeroDeGuia.desdeTexto(numero);
        Guia guia = repositorio.buscarPorNumero(numeroDeGuia)
                .orElseThrow(() -> new GuiaNoEncontradaException(numeroDeGuia));

        if (!operador.puedeOperarEn(guia.origen()) && !operador.puedeOperarEn(guia.destino())) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes ver el detalle de guías que salen de tu sucursal o llegan a ella.");
        }
        return guia;
    }
}

package mx.pecdmx.backend.aplicacion.guia;

import java.time.Clock;
import java.util.Objects;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.GuiaInvalidaException;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.guia.TransicionInvalidaException;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Operador;

/** Avanza una guía a un estado nuevo, en la sucursal donde está quien lo hace. */
public class ServicioDeCambioDeEstado implements CambiarEstadoDeGuia {

    private final RepositorioDeGuias repositorio;
    private final Clock reloj;

    public ServicioDeCambioDeEstado(RepositorioDeGuias repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Guia cambiar(SolicitudCambioDeEstado solicitud, Operador operador) {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
        Objects.requireNonNull(operador, "El operador es obligatorio");
        Sucursal lugar = dondeOcurre(solicitud.sucursal(), operador);

        NumeroDeGuia numero = NumeroDeGuia.desdeTexto(solicitud.numero());
        Guia guia = repositorio.buscarPorNumero(numero).orElseThrow(() -> new GuiaNoEncontradaException(numero));

        // Solo se envuelven los errores de ESTE cambio. Un historial dañado al leer la guía no pasa por aquí.
        try {
            guia.cambiarEstado(solicitud.estado(), lugar, solicitud.nota(), reloj.instant());
        } catch (TransicionInvalidaException | GuiaInvalidaException rechazo) {
            throw new CambioDeEstadoRechazadoException(rechazo.getMessage(), rechazo);
        }

        repositorio.guardar(guia);
        return guia;
    }

    /** El empleado opera en su sucursal; el gerente debe decir en cuál. */
    private static Sucursal dondeOcurre(Sucursal indicada, Operador operador) {
        if (operador.esGerente()) {
            if (indicada == null) {
                throw new SolicitudInvalidaException(
                        "Como gerente, indica la sucursal donde ocurre el evento.");
            }
            return indicada;
        }
        Sucursal propia = operador.sucursalAsignada().orElseThrow();
        if (indicada != null && indicada != propia) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes registrar eventos en tu sucursal (" + propia + ").");
        }
        return propia;
    }
}

package mx.pecdmx.backend.aplicacion.guia;

import java.time.Clock;
import java.util.Objects;
import mx.pecdmx.backend.aplicacion.cotizacion.CotizarEnvio;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.usuario.Operador;

/** Registra guías nuevas: cotiza, revisa el permiso, asigna número y guarda. */
public class ServicioDeRegistroDeGuias implements RegistrarGuia {

    private final CotizarEnvio cotizarEnvio;
    private final RepositorioDeGuias repositorio;
    private final Clock reloj;

    public ServicioDeRegistroDeGuias(CotizarEnvio cotizarEnvio, RepositorioDeGuias repositorio, Clock reloj) {
        this.cotizarEnvio = Objects.requireNonNull(cotizarEnvio);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Guia registrar(SolicitudRegistroDeGuia solicitud, Operador operador) {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
        Objects.requireNonNull(operador, "El operador es obligatorio");

        // El precio se calcula igual que en una cotización pública: una sola fuente de verdad.
        Cotizacion cotizacion = cotizarEnvio.cotizar(solicitud.envio());

        // El permiso se revisa antes de pedir un número, para no gastar números en intentos rechazados.
        if (!operador.puedeOperarEn(cotizacion.origen())) {
            throw new OperacionNoPermitidaException("Solo puedes registrar guías con origen en tu sucursal ("
                    + operador.sucursalAsignada().map(Object::toString).orElse("") + ").");
        }

        Guia guia = Guia.registrar(
                repositorio.siguienteNumero(), cotizacion, solicitud.nombreDestinatario(), reloj.instant());
        repositorio.guardar(guia);
        return guia;
    }
}

package mx.pecdmx.backend.dominio.guia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/**
 * Un envío de PECDMX, de sucursal a sucursal. Guarda la cotización cobrada, el destinatario y su historial.
 * El historial solo crece: nunca se borra ni se edita un evento.
 * La hora siempre llega desde afuera (no se lee el reloj aquí), para que las reglas se puedan probar.
 */
public final class Guia {

    public static final int LONGITUD_MAXIMA_NOMBRE = 100;

    /** Estados que solo pueden ocurrir en la sucursal de destino. */
    private static final Set<EstadoEnvio> SOLO_EN_DESTINO = EnumSet.of(EstadoEnvio.EN_SUCURSAL_DESTINO, EstadoEnvio.ENTREGADA);

    private final NumeroDeGuia numero;
    private final Cotizacion cotizacion;
    private final String nombreDestinatario;
    private final List<EventoDeRastreo> historial = new ArrayList<>();

    private Guia(NumeroDeGuia numero, Cotizacion cotizacion, String nombreDestinatario) {
        this.numero = Objects.requireNonNull(numero, "El número de guía es obligatorio");
        this.cotizacion = Objects.requireNonNull(cotizacion, "La cotización es obligatoria");
        this.nombreDestinatario = validarNombre(nombreDestinatario);
    }

    /** Crea una guía nueva con su primer evento: REGISTRADA en la sucursal de origen. */
    public static Guia registrar(
            NumeroDeGuia numero, Cotizacion cotizacion, String nombreDestinatario, Instant fechaHora) {
        Guia guia = new Guia(numero, cotizacion, nombreDestinatario);
        guia.historial.add(new EventoDeRastreo(
                EstadoEnvio.REGISTRADA, fechaHora, cotizacion.origen(), "Paquete recibido en sucursal"));
        return guia;
    }

    /**
     * Rearma una guía que ya existía (por ejemplo, leída de la base de datos) con su historial completo.
     * No crea eventos nuevos, pero vuelve a revisar el historial con las mismas reglas que {@link #cambiarEstado},
     * para detectar datos alterados.
     *
     * @throws GuiaInvalidaException si el historial está vacío, no empieza en REGISTRADA en el origen
     *     o algún evento rompe las reglas
     */
    public static Guia reconstruir(
            NumeroDeGuia numero, Cotizacion cotizacion, String nombreDestinatario, List<EventoDeRastreo> historial) {
        Guia guia = new Guia(numero, cotizacion, nombreDestinatario);
        if (historial == null || historial.isEmpty()) {
            throw historialDanado(numero, "no tiene eventos.");
        }

        EventoDeRastreo primero = historial.getFirst();
        if (primero.estado() != EstadoEnvio.REGISTRADA || primero.sucursal() != guia.origen()) {
            throw historialDanado(numero, "debe empezar en 'Registrada' en la sucursal de origen ("
                    + guia.origen().codigo() + ").");
        }
        guia.historial.add(primero);

        for (EventoDeRastreo evento : historial.subList(1, historial.size())) {
            try {
                guia.agregarEvento(evento);
            } catch (TransicionInvalidaException | GuiaInvalidaException error) {
                throw historialDanado(numero, error.getMessage());
            }
        }
        return guia;
    }

    /**
     * Avanza la guía a un estado nuevo y lo anota en el historial.
     *
     * @throws TransicionInvalidaException si el salto de estado no está permitido
     * @throws GuiaInvalidaException si la sucursal no corresponde o la fecha es anterior al último evento
     */
    public void cambiarEstado(EstadoEnvio nuevoEstado, Sucursal sucursal, String nota, Instant fechaHora) {
        agregarEvento(new EventoDeRastreo(nuevoEstado, fechaHora, sucursal, nota));
    }

    /** Las reglas para sumar un evento al historial. Las usan tanto {@link #cambiarEstado} como {@link #reconstruir}. */
    private void agregarEvento(EventoDeRastreo evento) {
        EstadoEnvio nuevoEstado = evento.estado();
        estadoActual().validarPasoA(nuevoEstado);
        if (SOLO_EN_DESTINO.contains(nuevoEstado) && evento.sucursal() != destino()) {
            throw new GuiaInvalidaException("El estado '" + nuevoEstado.nombre()
                    + "' solo puede registrarse en la sucursal de destino (" + destino().codigo()
                    + "), no en " + evento.sucursal().codigo() + ".");
        }
        if (evento.fechaHora().isBefore(ultimoEvento().fechaHora())) {
            throw new GuiaInvalidaException("La fecha del nuevo evento no puede ser anterior a la del último ("
                    + ultimoEvento().fechaHora() + ").");
        }

        historial.add(evento);
    }

    private static GuiaInvalidaException historialDanado(NumeroDeGuia numero, String detalle) {
        return new GuiaInvalidaException("El historial guardado de la guía " + numero + " no es válido: " + detalle);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new GuiaInvalidaException("El nombre del destinatario es obligatorio.");
        }
        String limpio = nombre.strip();
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new GuiaInvalidaException(
                    "El nombre del destinatario no puede tener más de " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    public NumeroDeGuia numero() {
        return numero;
    }

    public Cotizacion cotizacion() {
        return cotizacion;
    }

    public Sucursal origen() {
        return cotizacion.origen();
    }

    public Sucursal destino() {
        return cotizacion.destino();
    }

    public String nombreDestinatario() {
        return nombreDestinatario;
    }

    public EstadoEnvio estadoActual() {
        return ultimoEvento().estado();
    }

    public EventoDeRastreo ultimoEvento() {
        return historial.getLast();
    }

    /** Copia de solo lectura: quien la reciba no puede modificar el historial real. */
    public List<EventoDeRastreo> historial() {
        return List.copyOf(historial);
    }
}

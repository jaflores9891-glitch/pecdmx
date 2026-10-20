package mx.pecdmx.backend.dominio.guia;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Estados por los que pasa una guía y a cuáles puede avanzar desde cada uno.
 *
 * <pre>
 * REGISTRADA → EN_TRANSITO → EN_SUCURSAL_DESTINO → ENTREGADA
 *      │             ↕                ↕
 *  CANCELADA     INCIDENCIA (retraso, daño o extravío)
 * </pre>
 */
public enum EstadoEnvio {

    REGISTRADA("Registrada", "Recibimos tu paquete en la sucursal de origen."),
    EN_TRANSITO("En tránsito", "Tu paquete va en camino a la sucursal de destino."),
    EN_SUCURSAL_DESTINO("En sucursal de destino", "Tu paquete ya está listo para recogerse."),
    INCIDENCIA("Incidencia", "Hubo un problema con tu envío y lo estamos atendiendo."),
    ENTREGADA("Entregada", "El paquete se entregó al destinatario."),
    CANCELADA("Cancelada", "El envío se canceló antes de salir.");

    private final String nombre;
    private final String descripcion;

    EstadoEnvio(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Estados a los que se puede avanzar desde este. Vacío si el estado es final. */
    public Set<EstadoEnvio> siguientesPosibles() {
        // Un switch (y no un campo) porque un enum no puede nombrar en su constructor a los valores que vienen después.
        return switch (this) {
            case REGISTRADA -> EnumSet.of(EN_TRANSITO, CANCELADA);
            case EN_TRANSITO -> EnumSet.of(EN_SUCURSAL_DESTINO, INCIDENCIA);
            case EN_SUCURSAL_DESTINO -> EnumSet.of(ENTREGADA, INCIDENCIA);
            case INCIDENCIA -> EnumSet.of(EN_TRANSITO, EN_SUCURSAL_DESTINO);
            case ENTREGADA, CANCELADA -> EnumSet.noneOf(EstadoEnvio.class);
        };
    }

    public boolean puedePasarA(EstadoEnvio destino) {
        return siguientesPosibles().contains(destino);
    }

    public boolean esFinal() {
        return siguientesPosibles().isEmpty();
    }

    /** @throws TransicionInvalidaException si no se puede pasar de este estado a {@code destino} */
    public void validarPasoA(EstadoEnvio destino) {
        Objects.requireNonNull(destino, "El estado nuevo es obligatorio");
        if (!puedePasarA(destino)) {
            throw new TransicionInvalidaException(this, destino);
        }
    }
}

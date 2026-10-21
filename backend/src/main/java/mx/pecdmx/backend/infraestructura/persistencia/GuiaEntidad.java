package mx.pecdmx.backend.infraestructura.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EventoDeRastreo;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Zona;

/**
 * Una fila de la tabla {@code guias}, con sus eventos. Es solo un "molde" de la base de datos:
 * las reglas del negocio viven en {@link Guia}, y se traduce en {@link #desde} y {@link #aDominio}.
 */
@Entity
@Table(name = "guias")
class GuiaEntidad {

    @Id
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sucursal origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sucursal destino;

    @Column(nullable = false)
    private short zona;

    @Column(name = "peso_real_kg", nullable = false)
    private BigDecimal pesoRealKg;

    @Column(name = "peso_volumetrico_kg", nullable = false)
    private BigDecimal pesoVolumetricoKg;

    @Column(name = "peso_cobrable_kg", nullable = false)
    private int pesoCobrableKg;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal iva;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "nombre_destinatario", nullable = false)
    private String nombreDestinatario;

    /** Hibernate lo sube en cada cambio; si dos guardados chocan en el mismo instante, el segundo falla. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "guia", cascade = CascadeType.ALL)
    @OrderBy("orden")
    private List<EventoDeRastreoEntidad> eventos = new ArrayList<>();

    protected GuiaEntidad() {
    }

    static GuiaEntidad desde(Guia guia) {
        Cotizacion cotizacion = guia.cotizacion();
        GuiaEntidad entidad = new GuiaEntidad();
        entidad.numero = guia.numero().valor();
        entidad.origen = cotizacion.origen();
        entidad.destino = cotizacion.destino();
        entidad.zona = (short) cotizacion.zona().numero();
        entidad.pesoRealKg = cotizacion.pesoRealKg();
        entidad.pesoVolumetricoKg = cotizacion.pesoVolumetricoKg();
        entidad.pesoCobrableKg = cotizacion.pesoCobrableKg();
        entidad.subtotal = cotizacion.subtotal();
        entidad.iva = cotizacion.iva();
        entidad.total = cotizacion.total();
        entidad.nombreDestinatario = guia.nombreDestinatario();
        guia.historial().forEach(entidad::agregarEvento);
        return entidad;
    }

    Guia aDominio() {
        Cotizacion cotizacion = new Cotizacion(
                origen, destino, Zona.values()[zona - 1],
                pesoRealKg, pesoVolumetricoKg, pesoCobrableKg,
                subtotal, iva, total);
        return Guia.reconstruir(new NumeroDeGuia(numero), cotizacion, nombreDestinatario, eventosDominio());
    }

    List<EventoDeRastreo> eventosDominio() {
        return eventos.stream().map(EventoDeRastreoEntidad::aDominio).toList();
    }

    /** Agrega un evento al final del historial, con el siguiente número de orden (0, 1, 2...). */
    void agregarEvento(EventoDeRastreo evento) {
        eventos.add(new EventoDeRastreoEntidad(this, eventos.size(), evento));
    }
}

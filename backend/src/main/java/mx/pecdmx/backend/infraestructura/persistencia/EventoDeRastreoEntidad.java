package mx.pecdmx.backend.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.EventoDeRastreo;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/** Una fila de la tabla {@code eventos_rastreo}. Solo la usa el adaptador de persistencia. */
@Entity
@Table(name = "eventos_rastreo")
class EventoDeRastreoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "numero_guia")
    private GuiaEntidad guia;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEnvio estado;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private String nota;

    /** JPA necesita un constructor vacío; nadie más debe usarlo. */
    protected EventoDeRastreoEntidad() {
    }

    EventoDeRastreoEntidad(GuiaEntidad guia, int orden, EventoDeRastreo evento) {
        this.guia = guia;
        this.orden = orden;
        this.estado = evento.estado();
        this.fechaHora = evento.fechaHora();
        this.sucursal = evento.sucursal();
        this.nota = evento.nota();
    }

    EventoDeRastreo aDominio() {
        return new EventoDeRastreo(estado, fechaHora, sucursal, nota);
    }
}

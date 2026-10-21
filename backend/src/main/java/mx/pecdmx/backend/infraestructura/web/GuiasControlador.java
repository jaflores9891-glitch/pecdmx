package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.Valid;
import java.net.URI;
import mx.pecdmx.backend.aplicacion.guia.ConsultarDetalleDeGuia;
import mx.pecdmx.backend.aplicacion.guia.RastrearGuia;
import mx.pecdmx.backend.aplicacion.guia.RegistrarGuia;
import mx.pecdmx.backend.dominio.guia.Guia;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET  /api/guias/{numero}         → rastreo público.
 * POST /api/guias                  → registrar una guía (empleado o gerente).
 * GET  /api/guias/{numero}/detalle → detalle con destinatario y montos (empleado o gerente).
 */
@RestController
@RequestMapping("/api/guias")
class GuiasControlador {

    private final RastrearGuia rastrearGuia;
    private final RegistrarGuia registrarGuia;
    private final ConsultarDetalleDeGuia consultarDetalle;

    GuiasControlador(RastrearGuia rastrearGuia, RegistrarGuia registrarGuia, ConsultarDetalleDeGuia consultarDetalle) {
        this.rastrearGuia = rastrearGuia;
        this.registrarGuia = registrarGuia;
        this.consultarDetalle = consultarDetalle;
    }

    @GetMapping("/{numero}")
    RastreoJson rastrear(@PathVariable String numero) {
        return RastreoJson.de(rastrearGuia.rastrear(numero));
    }

    /** Responde 201 Created con la dirección de la guía nueva en el encabezado Location. */
    @PostMapping
    ResponseEntity<DetalleDeGuiaJson> registrar(
            @Valid @RequestBody SolicitudRegistroDeGuiaJson solicitud, @AuthenticationPrincipal Jwt gafete) {
        Guia guia = registrarGuia.registrar(solicitud.aSolicitud(), OperadorDelGafete.de(gafete));
        return ResponseEntity.created(URI.create("/api/guias/" + guia.numero().valor()))
                .body(DetalleDeGuiaJson.de(guia));
    }

    @GetMapping("/{numero}/detalle")
    DetalleDeGuiaJson detalle(@PathVariable String numero, @AuthenticationPrincipal Jwt gafete) {
        return DetalleDeGuiaJson.de(consultarDetalle.consultar(numero, OperadorDelGafete.de(gafete)));
    }
}

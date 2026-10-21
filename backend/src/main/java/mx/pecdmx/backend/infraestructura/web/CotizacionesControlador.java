package mx.pecdmx.backend.infraestructura.web;

import jakarta.validation.Valid;
import mx.pecdmx.backend.aplicacion.cotizacion.CotizarEnvio;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/cotizaciones: calcula el precio de un envío. No guarda nada. */
@RestController
@RequestMapping("/api/cotizaciones")
class CotizacionesControlador {

    private final CotizarEnvio cotizarEnvio;

    CotizacionesControlador(CotizarEnvio cotizarEnvio) {
        this.cotizarEnvio = cotizarEnvio;
    }

    @PostMapping
    CotizacionJson cotizar(@Valid @RequestBody SolicitudCotizacionJson solicitud) {
        return CotizacionJson.de(cotizarEnvio.cotizar(solicitud.aSolicitud()));
    }
}

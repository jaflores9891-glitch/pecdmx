package mx.pecdmx.backend.infraestructura.web;

import mx.pecdmx.backend.aplicacion.guia.RastrearGuia;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/guias/{numero}: rastreo público de una guía. */
@RestController
@RequestMapping("/api/guias")
class GuiasControlador {

    private final RastrearGuia rastrearGuia;

    GuiasControlador(RastrearGuia rastrearGuia) {
        this.rastrearGuia = rastrearGuia;
    }

    @GetMapping("/{numero}")
    RastreoJson rastrear(@PathVariable String numero) {
        return RastreoJson.de(rastrearGuia.rastrear(numero));
    }
}

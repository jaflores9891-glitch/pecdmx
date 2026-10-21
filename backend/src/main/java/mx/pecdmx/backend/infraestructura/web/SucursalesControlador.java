package mx.pecdmx.backend.infraestructura.web;

import java.util.Arrays;
import java.util.List;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/sucursales: la lista de sucursales, para los menús de la web y del chatbot. */
@RestController
@RequestMapping("/api/sucursales")
class SucursalesControlador {

    @GetMapping
    List<SucursalJson> listar() {
        return Arrays.stream(Sucursal.values()).map(SucursalJson::de).toList();
    }
}

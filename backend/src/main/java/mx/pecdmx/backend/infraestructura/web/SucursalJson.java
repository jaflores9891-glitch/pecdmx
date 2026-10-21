package mx.pecdmx.backend.infraestructura.web;

import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/** Cómo se ve una sucursal en el JSON de la API. */
record SucursalJson(String codigo, String ciudad, String estado) {

    static SucursalJson de(Sucursal sucursal) {
        return new SucursalJson(sucursal.codigo(), sucursal.ciudad(), sucursal.estado());
    }
}

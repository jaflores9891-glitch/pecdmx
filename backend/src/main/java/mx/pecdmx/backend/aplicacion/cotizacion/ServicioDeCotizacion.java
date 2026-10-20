package mx.pecdmx.backend.aplicacion.cotizacion;

import java.util.Objects;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;

/** Atiende las solicitudes de cotización: traduce los datos de afuera al dominio y calcula el precio. */
public class ServicioDeCotizacion implements CotizarEnvio {

    @Override
    public Cotizacion cotizar(SolicitudCotizacion solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud de cotización es obligatoria");

        Sucursal origen = Sucursal.desdeCodigo(solicitud.origen());
        Sucursal destino = Sucursal.desdeCodigo(solicitud.destino());
        Paquete paquete = new Paquete(
                solicitud.pesoKg(), solicitud.largoCm(), solicitud.anchoCm(), solicitud.altoCm());

        return Cotizacion.calcular(origen, destino, paquete);
    }
}

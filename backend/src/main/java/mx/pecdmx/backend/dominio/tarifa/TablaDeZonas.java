package mx.pecdmx.backend.dominio.tarifa;

import static mx.pecdmx.backend.dominio.sucursal.Sucursal.CDMX;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.GDL;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.MTY;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.PUE;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.QRO;
import static mx.pecdmx.backend.dominio.sucursal.Sucursal.TIJ;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;

/** Decide la zona de tarifa entre dos sucursales. Es simétrica: A → B es la misma zona que B → A. */
public final class TablaDeZonas {

    // Pares cercanos (menos de 400 km). Todos los demás pares sin Tijuana son zona 3.
    private static final Set<Set<Sucursal>> PARES_ZONA_2 = Set.of(
            EnumSet.of(CDMX, PUE),
            EnumSet.of(CDMX, QRO),
            EnumSet.of(PUE, QRO),
            EnumSet.of(GDL, QRO));

    private TablaDeZonas() {
    }

    public static Zona zonaEntre(Sucursal origen, Sucursal destino) {
        Objects.requireNonNull(origen, "La sucursal de origen es obligatoria");
        Objects.requireNonNull(destino, "La sucursal de destino es obligatoria");

        if (origen == destino) {
            return Zona.ZONA_1;
        }
        if (origen == TIJ || destino == TIJ) {
            return Zona.ZONA_4;
        }
        if (PARES_ZONA_2.contains(EnumSet.of(origen, destino))) {
            return Zona.ZONA_2;
        }
        return Zona.ZONA_3;
    }
}

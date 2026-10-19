package mx.pecdmx.backend.dominio.sucursal;

/** Se lanza cuando alguien pide una sucursal que PECDMX no tiene. */
public class SucursalDesconocidaException extends RuntimeException {

    public SucursalDesconocidaException(String codigo) {
        super("No existe una sucursal con el código '" + codigo
                + "'. Sucursales válidas: CDMX, GDL, MTY, PUE, QRO, TIJ.");
    }
}

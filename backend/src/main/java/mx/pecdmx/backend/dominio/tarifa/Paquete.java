package mx.pecdmx.backend.dominio.tarifa;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Un paquete a enviar: peso real en kg y medidas en cm.
 * Se valida al crearse, así que un {@code Paquete} que existe siempre es válido.
 */
public record Paquete(BigDecimal pesoKg, int largoCm, int anchoCm, int altoCm) {

    public static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("70");

    /** Estándar de la industria: centímetros cúbicos que equivalen a 1 kg. */
    public static final BigDecimal DIVISOR_VOLUMETRICO = new BigDecimal("5000");

    public Paquete {
        if (pesoKg == null || pesoKg.signum() <= 0) {
            throw new PaqueteInvalidoException("El peso debe ser mayor a 0 kg.");
        }
        if (pesoKg.compareTo(PESO_MAXIMO_KG) > 0) {
            throw new PaqueteInvalidoException("El peso máximo por paquete es de 70 kg.");
        }
        if (largoCm <= 0 || anchoCm <= 0 || altoCm <= 0) {
            throw new PaqueteInvalidoException("Las medidas (largo, ancho y alto) deben ser mayores a 0 cm.");
        }
    }

    /** largo × ancho × alto ÷ 5,000. Refleja el espacio que el paquete ocupa en el camión. */
    public BigDecimal pesoVolumetricoKg() {
        BigDecimal volumenCm3 = BigDecimal.valueOf((long) largoCm * anchoCm * altoCm);
        return volumenCm3.divide(DIVISOR_VOLUMETRICO, 2, RoundingMode.HALF_UP);
    }

    /** El mayor entre el peso real y el volumétrico, redondeado hacia arriba al kilo siguiente. */
    public int pesoCobrableKg() {
        BigDecimal mayor = pesoKg.max(pesoVolumetricoKg());
        return mayor.setScale(0, RoundingMode.CEILING).intValueExact();
    }
}

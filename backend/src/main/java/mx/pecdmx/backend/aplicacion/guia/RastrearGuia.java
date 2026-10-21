package mx.pecdmx.backend.aplicacion.guia;

/** Puerto de entrada: consultar públicamente en qué va una guía (lo usan la web y el chatbot). */
public interface RastrearGuia {

    /**
     * @param numero el número tal como lo escribe el cliente ("pec 261019004-9")
     * @throws mx.pecdmx.backend.dominio.guia.NumeroDeGuiaInvalidoException si el número está mal escrito
     * @throws GuiaNoEncontradaException si el número es válido pero no existe esa guía
     */
    Rastreo rastrear(String numero);
}

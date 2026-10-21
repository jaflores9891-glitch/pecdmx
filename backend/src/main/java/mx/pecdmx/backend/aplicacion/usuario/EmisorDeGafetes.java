package mx.pecdmx.backend.aplicacion.usuario;

import java.time.Instant;
import mx.pecdmx.backend.dominio.usuario.Usuario;

/** Puerto de salida: crear un gafete firmado para un usuario. Cómo se firma (JWT) lo decide un adaptador. */
public interface EmisorDeGafetes {

    Gafete emitir(Usuario usuario, Instant ahora);
}

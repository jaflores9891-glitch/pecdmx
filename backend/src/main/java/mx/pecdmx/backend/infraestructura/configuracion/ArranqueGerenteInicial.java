package mx.pecdmx.backend.infraestructura.configuracion;

import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Al arrancar el servicio, crea el gerente inicial si la base de datos no tiene usuarios. */
@Component
@EnableConfigurationProperties(PropiedadesGerenteInicial.class)
class ArranqueGerenteInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ArranqueGerenteInicial.class);

    private final ServicioDeGerenteInicial servicio;
    private final PropiedadesGerenteInicial propiedades;

    ArranqueGerenteInicial(ServicioDeGerenteInicial servicio, PropiedadesGerenteInicial propiedades) {
        this.servicio = servicio;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (!propiedades.estanCompletas()) {
            LOG.warn("No se configuró el gerente inicial (variables PECDMX_GERENTE_*). "
                    + "Si no hay usuarios, nadie podrá iniciar sesión.");
            return;
        }
        boolean creado = servicio.crearSiNoHayUsuarios(
                propiedades.correo(), propiedades.nombre(), propiedades.contrasena());
        if (creado) {
            LOG.info("Se creó el gerente inicial {}", propiedades.correo());
        }
    }
}

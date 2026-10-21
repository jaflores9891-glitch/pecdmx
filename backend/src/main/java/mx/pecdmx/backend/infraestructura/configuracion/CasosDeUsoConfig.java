package mx.pecdmx.backend.infraestructura.configuracion;

import mx.pecdmx.backend.aplicacion.cotizacion.CotizarEnvio;
import mx.pecdmx.backend.aplicacion.cotizacion.ServicioDeCotizacion;
import mx.pecdmx.backend.aplicacion.guia.RastrearGuia;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.aplicacion.guia.ServicioDeRastreo;
import mx.pecdmx.backend.aplicacion.usuario.CifradorDeContrasenas;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicial;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Le dice a Spring cómo armar los casos de uso y con qué adaptadores conectarlos.
 * Gracias a esto, las capas de dominio y aplicación no tienen ni una anotación de Spring.
 */
@Configuration
class CasosDeUsoConfig {

    @Bean
    CotizarEnvio cotizarEnvio() {
        return new ServicioDeCotizacion();
    }

    // Spring pasa aquí el adaptador de PostgreSQL, porque es el que implementa RepositorioDeGuias.
    @Bean
    RastrearGuia rastrearGuia(RepositorioDeGuias repositorioDeGuias) {
        return new ServicioDeRastreo(repositorioDeGuias);
    }

    @Bean
    ServicioDeGerenteInicial servicioDeGerenteInicial(
            RepositorioDeUsuarios repositorioDeUsuarios, CifradorDeContrasenas cifrador) {
        return new ServicioDeGerenteInicial(repositorioDeUsuarios, cifrador);
    }
}

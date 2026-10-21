package mx.pecdmx.backend.infraestructura.configuracion;

import java.time.Clock;
import mx.pecdmx.backend.aplicacion.cotizacion.CotizarEnvio;
import mx.pecdmx.backend.aplicacion.cotizacion.ServicioDeCotizacion;
import mx.pecdmx.backend.aplicacion.guia.CambiarEstadoDeGuia;
import mx.pecdmx.backend.aplicacion.guia.ConsultarDetalleDeGuia;
import mx.pecdmx.backend.aplicacion.guia.RastrearGuia;
import mx.pecdmx.backend.aplicacion.guia.RegistrarGuia;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.aplicacion.guia.ServicioDeCambioDeEstado;
import mx.pecdmx.backend.aplicacion.guia.ServicioDeDetalleDeGuias;
import mx.pecdmx.backend.aplicacion.guia.ServicioDeRastreo;
import mx.pecdmx.backend.aplicacion.guia.ServicioDeRegistroDeGuias;
import mx.pecdmx.backend.aplicacion.usuario.CifradorDeContrasenas;
import mx.pecdmx.backend.aplicacion.usuario.DarDeAltaEmpleado;
import mx.pecdmx.backend.aplicacion.usuario.EmisorDeGafetes;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeAltaDeEmpleados;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeGerenteInicial;
import mx.pecdmx.backend.aplicacion.usuario.ServicioDeSesiones;
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
    RegistrarGuia registrarGuia(CotizarEnvio cotizarEnvio, RepositorioDeGuias repositorioDeGuias, Clock reloj) {
        return new ServicioDeRegistroDeGuias(cotizarEnvio, repositorioDeGuias, reloj);
    }

    @Bean
    ConsultarDetalleDeGuia consultarDetalleDeGuia(RepositorioDeGuias repositorioDeGuias) {
        return new ServicioDeDetalleDeGuias(repositorioDeGuias);
    }

    @Bean
    CambiarEstadoDeGuia cambiarEstadoDeGuia(RepositorioDeGuias repositorioDeGuias, Clock reloj) {
        return new ServicioDeCambioDeEstado(repositorioDeGuias, reloj);
    }

    /** El reloj del sistema, en UTC. En los tests se puede cambiar por uno fijo. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    ServicioDeSesiones servicioDeSesiones(RepositorioDeUsuarios repositorioDeUsuarios,
            CifradorDeContrasenas cifrador, EmisorDeGafetes emisorDeGafetes, Clock reloj) {
        return new ServicioDeSesiones(repositorioDeUsuarios, cifrador, emisorDeGafetes, reloj);
    }

    @Bean
    DarDeAltaEmpleado darDeAltaEmpleado(RepositorioDeUsuarios repositorioDeUsuarios, CifradorDeContrasenas cifrador) {
        return new ServicioDeAltaDeEmpleados(repositorioDeUsuarios, cifrador);
    }

    @Bean
    ServicioDeGerenteInicial servicioDeGerenteInicial(
            RepositorioDeUsuarios repositorioDeUsuarios, CifradorDeContrasenas cifrador) {
        return new ServicioDeGerenteInicial(repositorioDeUsuarios, cifrador);
    }
}

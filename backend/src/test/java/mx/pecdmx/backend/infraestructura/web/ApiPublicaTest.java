package mx.pecdmx.backend.infraestructura.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.guia.RepositorioDeGuias;
import mx.pecdmx.backend.dominio.cotizacion.Cotizacion;
import mx.pecdmx.backend.dominio.guia.EstadoEnvio;
import mx.pecdmx.backend.dominio.guia.Guia;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.tarifa.Paquete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Prueba la API pública como la usarían la web y el chatbot, con la aplicación completa y PostgreSQL real. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApiPublicaTest {

    private static final Instant NUEVE_AM = Instant.parse("2026-10-20T15:00:00Z");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RepositorioDeGuias repositorio;

    // MockMvc simula peticiones HTTP sin abrir un servidor real.
    private MockMvc mvc;

    // springSecurity() hace que las peticiones pasen por el guardia, como en producción:
    // así se comprueba que todo lo de esta clase sigue abierto sin gafete.
    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void listaLasSeisSucursales() throws Exception {
        mvc.perform(get("/api/sucursales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[2].codigo").value("MTY"))
                .andExpect(jsonPath("$[2].ciudad").value("Monterrey"))
                .andExpect(jsonPath("$[2].estado").value("Nuevo León"));
    }

    @Test
    void cotizaElEjemploDelNegocio() throws Exception {
        String solicitud = """
                {"origen": "cdmx", "destino": "MTY", "pesoKg": 3.2, "largoCm": 40, "anchoCm": 30, "altoCm": 20}
                """;

        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen.codigo").value("CDMX"))
                .andExpect(jsonPath("$.destino.codigo").value("MTY"))
                .andExpect(jsonPath("$.zona").value(3))
                .andExpect(jsonPath("$.pesoCobrableKg").value(5))
                .andExpect(jsonPath("$.subtotal").value(310.00))
                .andExpect(jsonPath("$.iva").value(49.60))
                .andExpect(jsonPath("$.total").value(359.60))
                .andExpect(jsonPath("$.moneda").value("MXN"));
    }

    @Test
    void rastreaUnaGuiaConSuHistorialSinDatosPersonales() throws Exception {
        Cotizacion cotizacion = Cotizacion.calcular(
                Sucursal.CDMX, Sucursal.MTY, new Paquete(new BigDecimal("3.2"), 40, 30, 20));
        Guia guia = Guia.registrar(repositorio.siguienteNumero(), cotizacion, "Ana López", NUEVE_AM);
        guia.cambiarEstado(EstadoEnvio.EN_TRANSITO, Sucursal.CDMX, "Salió en ruta a Monterrey",
                NUEVE_AM.plus(Duration.ofHours(2)));
        repositorio.guardar(guia);

        // El cliente lo escribe en minúsculas: la API lo acepta igual.
        mvc.perform(get("/api/guias/" + guia.numero().valor().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(guia.numero().valor()))
                .andExpect(jsonPath("$.origen.codigo").value("CDMX"))
                .andExpect(jsonPath("$.destino.codigo").value("MTY"))
                .andExpect(jsonPath("$.estado.codigo").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.estado.nombre").value("En tránsito"))
                .andExpect(jsonPath("$.historial", hasSize(2)))
                .andExpect(jsonPath("$.historial[0].estado.codigo").value("REGISTRADA"))
                .andExpect(jsonPath("$.historial[0].fechaHora").value("2026-10-20T15:00:00Z"))
                .andExpect(jsonPath("$.historial[1].nota").value("Salió en ruta a Monterrey"))
                .andExpect(content().string(not(containsString("Ana López"))))
                .andExpect(content().string(not(containsString("359.60"))));
    }

    // --- Errores: cada uno con su código y un mensaje claro en español ---

    private ResultActions cotizar(String json) throws Exception {
        return mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void unaSucursalInventadaDa400ConSuMensaje() throws Exception {
        cotizar("""
                {"origen": "XYZ", "destino": "MTY", "pesoKg": 3, "largoCm": 10, "anchoCm": 10, "altoCm": 10}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.detail").value(containsString("'XYZ'")));
    }

    @Test
    void unPaqueteDeMasDe70KgDa400() throws Exception {
        cotizar("""
                {"origen": "CDMX", "destino": "MTY", "pesoKg": 80, "largoCm": 10, "anchoCm": 10, "altoCm": 10}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("70 kg")));
    }

    @Test
    void siFaltanDatosDa400ConLaListaDeLoQueFalta() throws Exception {
        cotizar("""
                {"origen": "CDMX", "largoCm": 10, "anchoCm": 10, "altoCm": 10}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos incompletos"))
                .andExpect(jsonPath("$.errores", hasSize(2)))
                .andExpect(jsonPath("$.errores", hasItem("El destino es obligatorio")))
                .andExpect(jsonPath("$.errores", hasItem("El peso es obligatorio")));
    }

    @Test
    void unJsonConTiposEquivocadosDa400() throws Exception {
        cotizar("""
                {"origen": "CDMX", "destino": "MTY", "pesoKg": "abc", "largoCm": 10, "anchoCm": 10, "altoCm": 10}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Formato inválido"));
    }

    @Test
    void unaGuiaMalEscritaDa400() throws Exception {
        mvc.perform(get("/api/guias/PEC2610190042"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Revisa que esté bien escrito")));
    }

    @Test
    void unaGuiaQueNoExisteDa404() throws Exception {
        String inexistente = repositorio.siguienteNumero().valor();

        mvc.perform(get("/api/guias/" + inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Guía no encontrada"))
                .andExpect(jsonPath("$.detail").value(containsString(inexistente)));
    }

    // Con gafete, porque sin él el guardia responde 401 a cualquier ruta no pública (ver SeguridadApiTest).
    @Test
    void unaUrlQueNoExisteSigueDando404YNo500() throws Exception {
        mvc.perform(get("/api/no-existe").with(jwt()))
                .andExpect(status().isNotFound());
    }
}

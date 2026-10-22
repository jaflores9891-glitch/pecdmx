package mx.pecdmx.backend.infraestructura.seguridad;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mx.pecdmx.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Simula lo que hace el navegador cuando una página de otro dominio quiere usar la API. */
@SpringBootTest(properties = "pecdmx.cors.origenes=https://pecdmx.vercel.app,http://localhost:5173")
@Import(TestcontainersConfiguration.class)
class CorsApiTest {

    private static final String VERCEL = "https://pecdmx.vercel.app";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // Antes de un POST, el navegador pregunta "¿puedo?" con una petición OPTIONS (verificación previa).
    @Test
    void laVerificacionPreviaDeUnaPaginaInvitadaRecibePermiso() throws Exception {
        mvc.perform(options("/api/cotizaciones")
                        .header(HttpHeaders.ORIGIN, VERCEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void unaPeticionNormalDeUnaPaginaInvitadaLlevaElPermiso() throws Exception {
        mvc.perform(get("/api/sucursales").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void unaPaginaDesconocidaEsRechazada() throws Exception {
        mvc.perform(options("/api/cotizaciones")
                        .header(HttpHeaders.ORIGIN, "https://sitio-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void noSePermitenMetodosQueLaWebNoUsa() throws Exception {
        mvc.perform(options("/api/guias/PEC0000000018")
                        .header(HttpHeaders.ORIGIN, VERCEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
    }

    // Sin el encabezado Origin (por ejemplo, curl o el chatbot desde su servidor), CORS no interviene.
    @Test
    void sinOrigenLaApiFuncionaIgual() throws Exception {
        mvc.perform(get("/api/sucursales"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

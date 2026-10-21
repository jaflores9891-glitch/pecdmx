package mx.pecdmx.backend.infraestructura.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import mx.pecdmx.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Registrar guías y ver su detalle, con gafetes de prueba de empleados y gerentes. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OperacionDeGuiasApiTest {

    private static final String ENVIO_CDMX_A_MTY = """
            {"origen": "CDMX", "destino": "MTY", "pesoKg": 3.2, "largoCm": 40, "anchoCm": 30, "altoCm": 20,
             "nombreDestinatario": "Ana López"}
            """;

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    /** Un gafete de prueba como el que emite /api/sesiones (sin pasar por el inicio de sesión). */
    private static JwtRequestPostProcessor gafeteDe(String rol, String sucursal) {
        return jwt()
                .jwt(gafete -> {
                    gafete.subject(rol.toLowerCase() + "@pecdmx.test").claim("rol", rol);
                    if (sucursal != null) {
                        gafete.claim("sucursal", sucursal);
                    }
                })
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private ResultActions registrar(String json, JwtRequestPostProcessor gafete) throws Exception {
        return mvc.perform(post("/api/guias").with(gafete).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String registrarYObtenerNumero() throws Exception {
        String respuesta = registrar(ENVIO_CDMX_A_MTY, gafeteDe("EMPLEADO", "CDMX"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.numero");
    }

    @Test
    void unEmpleadoRegistraUnaGuiaYRecibe201ConSuNumero() throws Exception {
        registrar(ENVIO_CDMX_A_MTY, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/guias/PEC")))
                .andExpect(jsonPath("$.numero").value(matchesPattern("PEC\\d{10}")))
                .andExpect(jsonPath("$.nombreDestinatario").value("Ana López"))
                .andExpect(jsonPath("$.cotizacion.total").value(359.60))
                .andExpect(jsonPath("$.estado.codigo").value("REGISTRADA"));
    }

    @Test
    void laGuiaRegistradaSePuedeRastrearPublicamente() throws Exception {
        String numero = registrarYObtenerNumero();

        mvc.perform(get("/api/guias/" + numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.codigo").value("REGISTRADA"));
    }

    @Test
    void unEmpleadoNoRegistraGuiasDeOtraSucursal() throws Exception {
        registrar(ENVIO_CDMX_A_MTY, gafeteDe("EMPLEADO", "PUE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Operación no permitida"))
                .andExpect(jsonPath("$.detail").value(containsString("(PUE)")));
    }

    @Test
    void unGerenteRegistraDesdeCualquierSucursal() throws Exception {
        registrar(ENVIO_CDMX_A_MTY, gafeteDe("GERENTE", null)).andExpect(status().isCreated());
    }

    @Test
    void sinGafeteNoSePuedeRegistrar() throws Exception {
        mvc.perform(post("/api/guias").contentType(MediaType.APPLICATION_JSON).content(ENVIO_CDMX_A_MTY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinNombreDeDestinatarioDa400() throws Exception {
        String sinNombre = """
                {"origen": "CDMX", "destino": "MTY", "pesoKg": 3.2, "largoCm": 40, "anchoCm": 30, "altoCm": 20}
                """;
        registrar(sinNombre, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("El nombre del destinatario es obligatorio")));
    }

    @Test
    void unNombreDeMasDe100CaracteresDa400() throws Exception {
        String nombreLargo = ENVIO_CDMX_A_MTY.replace("Ana López", "A".repeat(101));
        registrar(nombreLargo, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem(containsString("100 caracteres"))));
    }

    @Test
    void elDetalleLoVeElEmpleadoDelDestinoPeroNoUnoDeOtraSucursal() throws Exception {
        String numero = registrarYObtenerNumero();

        mvc.perform(get("/api/guias/" + numero + "/detalle").with(gafeteDe("EMPLEADO", "MTY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDestinatario").value("Ana López"))
                .andExpect(jsonPath("$.cotizacion.total").value(359.60));

        mvc.perform(get("/api/guias/" + numero + "/detalle").with(gafeteDe("EMPLEADO", "GDL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elDetalleSinGafeteDa401() throws Exception {
        mvc.perform(get("/api/guias/" + registrarYObtenerNumero() + "/detalle"))
                .andExpect(status().isUnauthorized());
    }
}

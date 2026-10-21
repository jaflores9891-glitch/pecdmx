package mx.pecdmx.backend.infraestructura.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Cambiar el estado de una guía por la API: el recorrido completo y sus errores. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CambioDeEstadoApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;
    private String numero;

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

    @BeforeEach
    void registrarGuiaCdmxAMty() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        String envio = """
                {"origen": "CDMX", "destino": "MTY", "pesoKg": 3.2, "largoCm": 40, "anchoCm": 30, "altoCm": 20,
                 "nombreDestinatario": "Ana López"}
                """;
        String respuesta = mvc.perform(post("/api/guias").with(gafeteDe("EMPLEADO", "CDMX"))
                        .contentType(MediaType.APPLICATION_JSON).content(envio))
                .andReturn().getResponse().getContentAsString();
        numero = JsonPath.read(respuesta, "$.numero");
    }

    private ResultActions cambiar(String json, JwtRequestPostProcessor gafete) throws Exception {
        return mvc.perform(post("/api/guias/" + numero + "/eventos").with(gafete)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void recorridoCompletoDeCdmxAMonterreyHastaLaEntrega() throws Exception {
        cambiar("""
                {"estado": "EN_TRANSITO", "nota": "Salió en ruta a Monterrey"}
                """, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.codigo").value("EN_TRANSITO"));

        cambiar("""
                {"estado": "EN_SUCURSAL_DESTINO", "nota": "Llegó a Monterrey"}
                """, gafeteDe("EMPLEADO", "MTY"))
                .andExpect(status().isOk());

        cambiar("""
                {"estado": "ENTREGADA", "nota": "Recogió la destinataria"}
                """, gafeteDe("EMPLEADO", "MTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.codigo").value("ENTREGADA"))
                .andExpect(jsonPath("$.historial", hasSize(4)));

        // El cliente ve todo el recorrido en el rastreo público.
        mvc.perform(get("/api/guias/" + numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.nombre").value("Entregada"))
                .andExpect(jsonPath("$.historial[1].sucursal.codigo").value("CDMX"))
                .andExpect(jsonPath("$.historial[2].nota").value("Llegó a Monterrey"))
                .andExpect(jsonPath("$.historial[3].sucursal.codigo").value("MTY"));
    }

    @Test
    void unGerenteIndicaLaSucursalDelEvento() throws Exception {
        cambiar("""
                {"estado": "EN_TRANSITO", "sucursal": "cdmx"}
                """, gafeteDe("GERENTE", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial[1].sucursal.codigo").value("CDMX"));
    }

    @Test
    void unGerenteSinSucursalDa400() throws Exception {
        cambiar("""
                {"estado": "EN_TRANSITO"}
                """, gafeteDe("GERENTE", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("indica la sucursal")));
    }

    @Test
    void unEstadoDesconocidoDa400ConLosEstadosValidos() throws Exception {
        cambiar("""
                {"estado": "VOLANDO"}
                """, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem(containsString("EN_TRANSITO"))));
    }

    @Test
    void unEmpleadoQueIndicaOtraSucursalDa403() throws Exception {
        cambiar("""
                {"estado": "EN_TRANSITO", "sucursal": "PUE"}
                """, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unSaltoProhibidoDa409() throws Exception {
        cambiar("""
                {"estado": "ENTREGADA"}
                """, gafeteDe("EMPLEADO", "MTY"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cambio de estado rechazado"))
                .andExpect(jsonPath("$.detail").value(containsString("no puede pasar a 'Entregada'")));
    }

    @Test
    void sinGafeteDa401() throws Exception {
        mvc.perform(post("/api/guias/" + numero + "/eventos")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"EN_TRANSITO\"}"))
                .andExpect(status().isUnauthorized());
    }
}

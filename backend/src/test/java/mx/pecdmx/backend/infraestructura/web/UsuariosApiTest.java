package mx.pecdmx.backend.infraestructura.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import mx.pecdmx.backend.TestcontainersConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Alta de empleados por la API, incluido el recorrido de punta a punta: alta → inicio de sesión → registrar guía. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UsuariosApiTest {

    private static final String CONTRASENA_INICIAL = "contrasena-inicial-de-prueba";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    // Un correo distinto en cada test, porque todos comparten la misma base de datos de prueba.
    private final String correoNuevo = "empleado." + UUID.randomUUID() + "@pecdmx.test";

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

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

    private ResultActions darDeAlta(String correo, String contrasena, JwtRequestPostProcessor gafete) throws Exception {
        String cuerpo = """
                {"correo": "%s", "nombre": "Luis Pérez", "sucursal": "mty", "contrasenaInicial": "%s"}
                """.formatted(correo, contrasena);
        var peticion = post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(gafete == null ? peticion : peticion.with(gafete));
    }

    @Test
    void deDarDeAltaARegistrarSuPrimeraGuia() throws Exception {
        // 1. La gerente da de alta a Luis en Monterrey.
        darDeAlta(correoNuevo, CONTRASENA_INICIAL, gafeteDe("GERENTE", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value(correoNuevo))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.sucursal").value("MTY"))
                // La respuesta nunca trae la contraseña, ni cifrada.
                .andExpect(content().string(Matchers.not(containsString("contrasena"))))
                .andExpect(content().string(Matchers.not(containsString(CONTRASENA_INICIAL))));

        // 2. Luis inicia sesión con su contraseña inicial y recibe un gafete REAL (firmado por el backend).
        String sesion = mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"correo": "%s", "contrasena": "%s"}
                        """.formatted(correoNuevo, CONTRASENA_INICIAL)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String gafete = "Bearer " + JsonPath.read(sesion, "$.gafete");

        mvc.perform(get("/api/sesiones/yo").header(HttpHeaders.AUTHORIZATION, gafete))
                .andExpect(jsonPath("$.sucursal").value("MTY"));

        // 3. Con ese gafete, Luis registra una guía que sale de Monterrey.
        mvc.perform(post("/api/guias").header(HttpHeaders.AUTHORIZATION, gafete)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"origen": "MTY", "destino": "TIJ", "pesoKg": 2, "largoCm": 20, "anchoCm": 20,
                                 "altoCm": 20, "nombreDestinatario": "Rosa Díaz"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cotizacion.zona").value(4));

        // ...pero no una que sale de la Ciudad de México.
        mvc.perform(post("/api/guias").header(HttpHeaders.AUTHORIZATION, gafete)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"origen": "CDMX", "destino": "TIJ", "pesoKg": 2, "largoCm": 20, "anchoCm": 20,
                                 "altoCm": 20, "nombreDestinatario": "Rosa Díaz"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEmpleadoNoPuedeDarDeAltaUsuarios() throws Exception {
        darDeAlta(correoNuevo, CONTRASENA_INICIAL, gafeteDe("EMPLEADO", "CDMX"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinGafeteDa401() throws Exception {
        darDeAlta(correoNuevo, CONTRASENA_INICIAL, null).andExpect(status().isUnauthorized());
    }

    @Test
    void unCorreoRepetidoDa409() throws Exception {
        darDeAlta(correoNuevo, CONTRASENA_INICIAL, gafeteDe("GERENTE", null)).andExpect(status().isCreated());

        darDeAlta(correoNuevo.toUpperCase(), CONTRASENA_INICIAL, gafeteDe("GERENTE", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Usuario existente"));
    }

    @Test
    void unaContrasenaCortaDa400() throws Exception {
        darDeAlta(correoNuevo, "corta-123", gafeteDe("GERENTE", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("12 caracteres")));
    }
}

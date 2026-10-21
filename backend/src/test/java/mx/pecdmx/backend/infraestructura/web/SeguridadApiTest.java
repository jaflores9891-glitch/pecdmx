package mx.pecdmx.backend.infraestructura.web;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import mx.pecdmx.backend.TestcontainersConfiguration;
import mx.pecdmx.backend.aplicacion.usuario.CifradorDeContrasenas;
import mx.pecdmx.backend.aplicacion.usuario.RepositorioDeUsuarios;
import mx.pecdmx.backend.dominio.sucursal.Sucursal;
import mx.pecdmx.backend.dominio.usuario.Correo;
import mx.pecdmx.backend.dominio.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Prueba el guardia completo: inicio de sesión, gafetes válidos, falsos y vencidos. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeguridadApiTest {

    private static final String CONTRASENA = "contrasena-de-prueba-de-luis";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RepositorioDeUsuarios repositorio;

    @Autowired
    private CifradorDeContrasenas cifrador;

    @Autowired
    private JwtEncoder codificadorReal;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        repositorio.guardar(Usuario.empleado(
                new Correo("luis.seguridad@pecdmx.test"), "Luis Pérez", Sucursal.MTY, cifrador.cifrar(CONTRASENA)));
    }

    private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = "{\"correo\": \"" + correo + "\", \"contrasena\": \"" + contrasena + "\"}";
        return mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String gafeteValido() throws Exception {
        String respuesta = iniciarSesion("luis.seguridad@pecdmx.test", CONTRASENA)
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.gafete");
    }

    private ResultActions quienSoy(String gafete) throws Exception {
        return mvc.perform(get("/api/sesiones/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + gafete));
    }

    @Test
    void conDatosCorrectosEntregaUnGafeteDe8Horas() throws Exception {
        iniciarSesion("luis.seguridad@pecdmx.test", CONTRASENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gafete", notNullValue()))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn", notNullValue()));
    }

    @Test
    void conElGafeteSePuedeEntrarAUnaRutaProtegida() throws Exception {
        quienSoy(gafeteValido())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("luis.seguridad@pecdmx.test"))
                .andExpect(jsonPath("$.nombre").value("Luis Pérez"))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.sucursal").value("MTY"));
    }

    @Test
    void conContrasenaEquivocadaDa401() throws Exception {
        iniciarSesion("luis.seguridad@pecdmx.test", "contrasena-equivocada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void conUnCorreoQueNoExisteDaElMismo401() throws Exception {
        iniciarSesion("nadie@pecdmx.test", CONTRASENA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void lasRutasPublicasSiguenAbiertasSinGafete() throws Exception {
        mvc.perform(get("/api/sucursales")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void unaRutaProtegidaSinGafeteDa401() throws Exception {
        mvc.perform(get("/api/sesiones/yo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/no-existe")).andExpect(status().isUnauthorized());
    }

    @Test
    void unGafeteMalFormadoDa401() throws Exception {
        quienSoy("esto-no-es-un-gafete").andExpect(status().isUnauthorized());
    }

    @Test
    void unGafeteFirmadoConOtraLlaveDa401() throws Exception {
        // Alguien arma un gafete de GERENTE, pero no conoce nuestra llave secreta.
        var llaveFalsa = new SecretKeySpec(
                "llave-inventada-por-un-atacante-0123456789".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String gafeteFalso = new NimbusJwtEncoder(new ImmutableSecret<>(llaveFalsa))
                .encode(parametros("intruso@pecdmx.test", Instant.now().plus(Duration.ofHours(1))))
                .getTokenValue();

        quienSoy(gafeteFalso).andExpect(status().isUnauthorized());
    }

    @Test
    void unGafeteVencidoDa401() throws Exception {
        // Firmado con la llave correcta, pero venció hace una hora.
        String gafeteVencido = codificadorReal
                .encode(parametros("luis.seguridad@pecdmx.test", Instant.now().minus(Duration.ofHours(1))))
                .getTokenValue();

        quienSoy(gafeteVencido).andExpect(status().isUnauthorized());
    }

    private static JwtEncoderParameters parametros(String correo, Instant expiraEn) {
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .issuer("pecdmx")
                .subject(correo)
                .issuedAt(expiraEn.minus(Duration.ofHours(8)))
                .expiresAt(expiraEn)
                .claim("rol", "GERENTE")
                .build();
        return JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos);
    }
}

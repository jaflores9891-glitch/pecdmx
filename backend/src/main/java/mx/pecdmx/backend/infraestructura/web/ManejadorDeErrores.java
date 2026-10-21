package mx.pecdmx.backend.infraestructura.web;

import java.util.List;
import mx.pecdmx.backend.aplicacion.guia.GuiaNoEncontradaException;
import mx.pecdmx.backend.aplicacion.usuario.CredencialesInvalidasException;
import mx.pecdmx.backend.dominio.guia.NumeroDeGuiaInvalidoException;
import mx.pecdmx.backend.dominio.sucursal.SucursalDesconocidaException;
import mx.pecdmx.backend.dominio.tarifa.PaqueteInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte los errores de todos los controladores en respuestas "Problem Details" (RFC 9457) en español.
 * Hereda de {@link ResponseEntityExceptionHandler} para que los errores de protocolo (URL inexistente,
 * método no permitido...) conserven su código correcto en vez de caer en el 500 genérico.
 */
@RestControllerAdvice
class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeErrores.class);

    /** Reglas del negocio rotas por los datos que mandó el cliente: 400 con el mensaje del dominio. */
    @ExceptionHandler({
        SucursalDesconocidaException.class,
        PaqueteInvalidoException.class,
        NumeroDeGuiaInvalidoException.class
    })
    ProblemDetail datosInvalidos(RuntimeException error) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", error.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credencialesInvalidas(CredencialesInvalidasException error) {
        return problema(HttpStatus.UNAUTHORIZED, "No autorizado", error.getMessage());
    }

    @ExceptionHandler(GuiaNoEncontradaException.class)
    ProblemDetail guiaNoEncontrada(GuiaNoEncontradaException error) {
        return problema(HttpStatus.NOT_FOUND, "Guía no encontrada", error.getMessage());
    }

    /** Cualquier error no previsto: el cliente recibe un mensaje genérico y el detalle técnico va solo al registro. */
    @ExceptionHandler(Exception.class)
    ProblemDetail errorInesperado(Exception error) {
        LOG.error("Error inesperado al atender una petición", error);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado",
                "Ocurrió un error inesperado. Intenta de nuevo en unos minutos.");
    }

    /** Faltan campos obligatorios en el JSON (las anotaciones @NotNull, @NotBlank de los DTOs). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errores = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getDefaultMessage())
                .sorted()
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos incompletos",
                "Faltan datos obligatorios en la petición.");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    /** El cuerpo no es un JSON válido o trae tipos equivocados (ej. "pesoKg": "abc"). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Formato inválido",
                "La petición no tiene un formato válido. Revisa que sea JSON y que los números sean números."));
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}

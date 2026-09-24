package com.checkout.backend.exceptions;

import com.checkout.backend.exceptions.dto.ErrorResponseDTO;
import com.checkout.backend.exceptions.dto.ErrorResponseDTO.FieldErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce cualquier excepcion que salga de un controller a una respuesta JSON
 * con la forma de ErrorResponseDTO.
 *
 * Sin esta clase Spring responde los errores con su propio formato por defecto,
 * y las excepciones no previstas salen como un 500 con un cuerpo que incluye el
 * nombre de la clase Java que fallo. El front tendria que parsear dos formas
 * distintas de error segun quien lo genero, y los detalles internos se filtran.
 *
 * Dos reglas que se siguen en todos los handlers de abajo:
 *
 * 1. El mensaje de un 4xx describe que hizo mal el cliente; el de un 5xx es
 *    siempre generico. Un 500 significa que el problema es nuestro, y el
 *    mensaje real de la excepcion puede traer nombres de tablas, rutas o
 *    fragmentos de query. Eso va al log, no al cuerpo de la respuesta.
 *
 * 2. Solo se loguea con stack trace lo que un desarrollador tiene que ir a
 *    mirar. Un 404 o un 400 son operacion normal: se registran en DEBUG. Un 500
 *    es un defecto y va en ERROR.
 *
 * Alcance: un @RestControllerAdvice solo ve lo que pasa por el
 * DispatcherServlet. Lo que falla antes, dentro de la cadena de filtros de
 * Spring Security (por ejemplo un JWT invalido rechazado por el filtro de
 * autenticacion), nunca llega hasta aca; ese caso se cubre con un
 * AuthenticationEntryPoint y un AccessDeniedHandler propios, que pertenecen a
 * la configuracion de seguridad.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Mensaje unico para todos los 500. Ver la regla 1 del javadoc de la clase.
     */
    private static final String INTERNAL_ERROR_MESSAGE =
            "Ocurrio un error interno. Intentalo de nuevo mas tarde.";

    // ---------------------------------------------------------------------
    // Excepciones propias
    // ---------------------------------------------------------------------

    /**
     * Cubre de una sola vez toda la jerarquia de ApiException, porque cada
     * subclase ya sabe con que codigo quiere salir. Agregar una excepcion de
     * negocio nueva no requiere tocar esta clase.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleApiException(ApiException ex,
                                                               HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    // ---------------------------------------------------------------------
    // 400 - el request no es utilizable
    // ---------------------------------------------------------------------

    /**
     * Falla un @Valid sobre un @RequestBody.
     *
     * Es el 400 mas frecuente de la API y el unico que aprovecha fieldErrors:
     * el cuerpo enumera cada campo rechazado con su motivo, para que el front
     * pueda marcar los inputs en vez de mostrar un cartel generico.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<FieldErrorDTO> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();

        log.debug("Validacion fallida en {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(
                        HttpStatus.BAD_REQUEST,
                        "La solicitud contiene campos invalidos.",
                        request.getRequestURI(),
                        fieldErrors));
    }

    /**
     * Falla una restriccion declarada fuera de un @RequestBody: un @Min sobre un
     * @PathVariable, o un @NotBlank sobre un @RequestParam. Spring no las agrupa
     * en un BindingResult, asi que el detalle por campo hay que armarlo a mano
     * desde el property path de cada violacion.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        List<FieldErrorDTO> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDTO(
                        lastNode(String.valueOf(violation.getPropertyPath())),
                        violation.getMessage()))
                .toList();

        log.debug("Restriccion violada en {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(
                        HttpStatus.BAD_REQUEST,
                        "La solicitud contiene parametros invalidos.",
                        request.getRequestURI(),
                        fieldErrors));
    }

    /**
     * El cuerpo no se pudo deserializar: JSON mal formado, un enum con un valor
     * que no existe, un numero donde se esperaba una fecha.
     *
     * El mensaje de Jackson no se reenvia: incluye la clase Java destino y la
     * posicion en el stream, que no le sirven a quien consume la API y si
     * describen la estructura interna.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        log.debug("Cuerpo ilegible en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no se pudo leer. Revisa que sea JSON valido.", request);
    }

    /**
     * Falta un @RequestParam obligatorio.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'.", request);
    }

    /**
     * Un parametro llego con un tipo que no se puede convertir, tipicamente
     * /usuarios/abc cuando el id es Long.
     *
     * Nombrar el parametro y el valor recibido es seguro: los dos los mando el
     * cliente. El tipo esperado se saca del metodo del controller, y puede venir
     * en null cuando el parametro no esta tipado, de ahi el chequeo.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        Class<?> expected = ex.getRequiredType();
        String message = "El valor '" + ex.getValue() + "' no es valido para el parametro '"
                + ex.getName() + "'"
                + (expected != null ? ", se esperaba " + expected.getSimpleName() : "")
                + ".";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // ---------------------------------------------------------------------
    // 401 / 403 - seguridad
    // ---------------------------------------------------------------------

    /**
     * Fallo de autenticacion levantado dentro de un controller, por ejemplo un
     * BadCredentialsException del AuthenticationManager en el endpoint de login.
     *
     * El mensaje no distingue entre correo inexistente y contrasena incorrecta.
     * Hacerlo convierte al login en un verificador de que cuentas existen.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        log.debug("Autenticacion fallida en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", request);
    }

    /**
     * El usuario esta autenticado y no tiene permiso. Es la excepcion que lanza
     * @PreAuthorize cuando la expresion da false.
     *
     * Atrapandola aca el 403 sale con el mismo formato que el resto de los
     * errores, en vez del cuerpo por defecto del contenedor.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        log.debug("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion.", request);
    }

    // ---------------------------------------------------------------------
    // 404 / 405 - la ruta
    // ---------------------------------------------------------------------

    /**
     * No hay ningun handler para la URL pedida. Sin esto Spring responde su
     * propia pagina de error y el 404 por ruta inexistente tendria otra forma
     * que el 404 por recurso inexistente.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(NoResourceFoundException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", request);
    }

    /**
     * La ruta existe pero no para ese verbo, por ejemplo un DELETE contra un
     * endpoint que solo define GET.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "El metodo " + ex.getMethod() + " no esta permitido en esta ruta.", request);
    }

    // ---------------------------------------------------------------------
    // 409 - conflicto con el estado guardado
    // ---------------------------------------------------------------------

    /**
     * Salto un constraint de la base: un UNIQUE o una foreign key.
     *
     * Es la red de seguridad del chequeo que hace el service. Cuando dos
     * registros con el mismo correo entran a la vez, los dos pasan la validacion
     * previa y el UNIQUE decide; ese perdedor termina aca y recibe el mismo 409
     * que si hubiera llegado solo.
     *
     * Va en WARN y no en DEBUG: puede ser una carrera normal, pero tambien una
     * validacion faltante en el service, y en ese caso conviene verlo.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        log.warn("Violacion de integridad en {}", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT,
                "La operacion entra en conflicto con datos ya registrados.", request);
    }

    // ---------------------------------------------------------------------
    // 500 - todo lo demas
    // ---------------------------------------------------------------------

    /**
     * Ultimo recurso: cualquier excepcion que no matcheo con ningun handler de
     * arriba.
     *
     * Spring elige siempre el handler mas especifico, asi que tener este metodo
     * no oculta a los anteriores. Lo que si hace es garantizar que ninguna
     * excepcion inesperada salga con el formato por defecto ni arrastre su
     * mensaje interno al cliente. El stack trace completo va al log, que es
     * donde hay que buscarlo.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex,
                                                             HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE, request);
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private static ResponseEntity<ErrorResponseDTO> build(HttpStatus status, String message,
                                                          HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDTO.of(status, message, request.getRequestURI()));
    }

    private static FieldErrorDTO toFieldError(FieldError error) {
        String message = error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "Valor invalido.";
        return new FieldErrorDTO(error.getField(), message);
    }

    /**
     * Se queda con el ultimo tramo del property path de una violacion.
     *
     * Jakarta Validation lo entrega como "metodo.argumento.campo" — por ejemplo
     * "buscarUsuario.id" — y al cliente solo le interesa "id", que es el nombre
     * que el mismo mando.
     */
    private static String lastNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }

}

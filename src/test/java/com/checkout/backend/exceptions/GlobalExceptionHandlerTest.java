package com.checkout.backend.exceptions;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que cada excepcion sale con el codigo HTTP correcto y con el cuerpo
 * de ErrorResponseDTO.
 *
 * El proyecto todavia no tiene controllers, asi que la prueba trae el suyo:
 * ThrowingController existe solo aca y cada endpoint lanza una excepcion
 * distinta. Se monta con standaloneSetup en vez de @WebMvcTest porque lo que se
 * esta probando es el advice, no la configuracion de Spring: sin contexto ni
 * cadena de seguridad, el test corre en milisegundos y no se rompe cuando
 * alguien toque la configuracion de seguridad mas adelante.
 *
 * La contracara es que lo que ocurre dentro de los filtros de Spring Security
 * no se cubre aca. No es un olvido: un @RestControllerAdvice tampoco lo ve en
 * produccion (ver el javadoc de GlobalExceptionHandler), y ese caso lo tiene
 * que cubrir el test de la configuracion de seguridad cuando exista.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * El SecurityContextHolder guarda el Authentication en un ThreadLocal, y los
     * tests de 401/403 lo escriben. Sin esta limpieza el que corre despues
     * heredaria el usuario del anterior segun el orden de ejecucion, que JUnit no
     * garantiza.
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("404: ResourceNotFoundException sale con el cuerpo estandar completo")
    void resourceNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado: 42"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                // fieldErrors es NON_EMPTY: no debe aparecer si no hubo validacion
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("400: una regla de negocio devuelve el mensaje que puso el service")
    void invalidRequest() throws Exception {
        mockMvc.perform(get("/test/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El aporte supera lo que falta para la meta."));
    }

    @Test
    @DisplayName("401: el mensaje no revela si el correo existe")
    void unauthenticated() throws Exception {
        mockMvc.perform(post("/test/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas."))
                // el mensaje original decia que la cuenta no existe: no puede salir
                .andExpect(jsonPath("$.message").value(not(containsString("no existe"))));
    }

    @Test
    @DisplayName("401: acceso denegado sin usuario autenticado no es 403")
    void accessDeniedWithoutAuthenticationIsUnauthorized() throws Exception {
        // Sin Authentication en el contexto: el cliente no se identifico.
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Debes autenticarte para acceder a este recurso."));
    }

    @Test
    @DisplayName("401: un token anonimo tampoco cuenta como autenticado")
    void anonymousTokenIsNotAuthenticated() throws Exception {
        // isAuthenticated() de un AnonymousAuthenticationToken devuelve true, asi
        // que sin el chequeo de tipo este caso saldria 403.
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Debes autenticarte para acceder a este recurso."));
    }

    @Test
    @DisplayName("403: acceso denegado con usuario autenticado si es 403")
    void accessDeniedWithAuthenticatedUserIsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "ana@utec.edu.pe", null, AuthorityUtils.createAuthorityList("ROLE_USER")));

        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta accion."));
    }

    @Test
    @DisplayName("401: UnauthenticatedException propia")
    void unauthenticatedException() throws Exception {
        mockMvc.perform(get("/test/token-vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("El token expiro."));
    }

    @Test
    @DisplayName("403: ForbiddenException propia sale con su propio mensaje")
    void forbiddenException() throws Exception {
        mockMvc.perform(get("/test/not-owner"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("La meta pertenece a otro usuario."));
    }

    @Test
    @DisplayName("409: un duplicado detectado por el service")
    void duplicate() throws Exception {
        mockMvc.perform(post("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El correo ya esta registrado."));
    }

    @Test
    @DisplayName("409: el mismo codigo cuando quien detecta el duplicado es la base")
    void dataIntegrityViolationBecomesConflict() throws Exception {
        mockMvc.perform(post("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La operacion entra en conflicto con datos ya registrados."))
                // el detalle del constraint es interno y no puede salir
                .andExpect(jsonPath("$.message").value(not(containsString("uk_user_email"))));
    }

    @Test
    @DisplayName("400: un @Valid fallido enumera cada campo rechazado")
    void bodyValidationListsFields() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene campos invalidos."))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]", hasSize(1)));
    }

    @Test
    @DisplayName("400: una violacion fuera del body reporta el campo sin el prefijo del metodo")
    void constraintViolationTrimsPropertyPath() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene parametros invalidos."))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                // el property path de la violacion es "name", no "metodo.dto.name"
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("400: JSON mal formado no filtra el mensaje de Jackson")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la solicitud no se pudo leer. Revisa que sea JSON valido."));
    }

    @Test
    @DisplayName("400: falta un parametro obligatorio y se lo nombra")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parametro obligatorio 'page'."));
    }

    @Test
    @DisplayName("400: un id que no es numero no llega al controller")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/test/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("'abc'")))
                .andExpect(jsonPath("$.message").value(containsString("Long")));
    }

    @Test
    @DisplayName("500: una excepcion inesperada no arrastra su mensaje interno")
    void unexpectedExceptionIsMasked() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("Ocurrio un error interno. Intentalo de nuevo mas tarde."))
                .andExpect(jsonPath("$.message").value(not(containsString("jdbc:postgresql"))));
    }

    @Test
    @DisplayName("Una excepcion con status propio 4xx conserva su codigo y su motivo")
    void responseStatusExceptionKeepsClientStatus() throws Exception {
        mockMvc.perform(get("/test/gone"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message").value("Esta meta fue eliminada."));
    }

    @Test
    @DisplayName("Una excepcion con status propio 5xx conserva el codigo pero no el detalle")
    void responseStatusExceptionMasksServerDetail() throws Exception {
        mockMvc.perform(get("/test/bad-gateway"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message")
                        .value("Ocurrio un error interno. Intentalo de nuevo mas tarde."))
                // la direccion del servicio interno no puede salir
                .andExpect(jsonPath("$.message").value(not(containsString("10.0.0.5"))));
    }

    @Test
    @DisplayName("500: una path variable que el mapping no declara es un bug nuestro, no un 400")
    void missingPathVariableIsServerError() throws Exception {
        mockMvc.perform(get("/test/sin-plantilla"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message")
                        .value("Ocurrio un error interno. Intentalo de nuevo mas tarde."));
    }

    // ------------------------------------------------------------------
    // Andamiaje del test
    // ------------------------------------------------------------------

    /**
     * Controller que existe solo para lanzar excepciones. Cada endpoint es el
     * disparador de un handler distinto del advice.
     */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Usuario", 42L);
        }

        @GetMapping("/test/invalid")
        void invalid() {
            throw new InvalidRequestException("El aporte supera lo que falta para la meta.");
        }

        @PostMapping("/test/login")
        void login() {
            // Mensaje deliberadamente indiscreto: el handler tiene que taparlo.
            throw new BadCredentialsException("No existe una cuenta con el correo ana@utec.edu.pe");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/test/not-owner")
        void notOwner() {
            throw new ForbiddenException("La meta pertenece a otro usuario.");
        }

        @PostMapping("/test/duplicate")
        void duplicate() {
            throw new DuplicateResourceException("El correo ya esta registrado.");
        }

        @PostMapping("/test/integrity")
        void integrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"uk_user_email\"");
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody SampleRequest request) {
        }

        /**
         * Lanza la ConstraintViolationException a mano en vez de apoyarse en
         * @Validated: standaloneSetup no crea el proxy que intercepta el metodo,
         * asi que la unica forma de llegar al handler desde aca es construir la
         * violacion con el validador y lanzarla.
         */
        @GetMapping("/test/constraint")
        void constraint() {
            try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                Validator validator = factory.getValidator();
                Set<jakarta.validation.ConstraintViolation<SampleRequest>> violations =
                        validator.validate(new SampleRequest("", "ana@utec.edu.pe"));
                throw new ConstraintViolationException(violations);
            }
        }

        @GetMapping("/test/param")
        void param(@RequestParam int page) {
        }

        @GetMapping("/test/typed/{id}")
        void typed(@PathVariable Long id) {
        }

        @GetMapping("/test/token-vencido")
        void tokenVencido() {
            throw new UnauthenticatedException("El token expiro.");
        }

        @GetMapping("/test/gone")
        void gone() {
            throw new ResponseStatusException(HttpStatus.GONE, "Esta meta fue eliminada.");
        }

        @GetMapping("/test/bad-gateway")
        void badGateway() {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "el servicio de cotizaciones en 10.0.0.5:8080 no responde");
        }

        /**
         * La firma pide una path variable que la plantilla de la URI no declara.
         * Spring lanza MissingPathVariableException, que es la unica subclase de
         * MissingRequestValueException que no es culpa del cliente.
         */
        @GetMapping("/test/sin-plantilla")
        void sinPlantilla(@PathVariable Long id) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException(
                    "Connection refused to jdbc:postgresql://localhost:5432/checkout");
        }
    }

    /**
     * Request minimo con dos reglas de validacion, una por campo, para que el
     * test de fieldErrors pueda comprobar que salen las dos.
     */
    record SampleRequest(
            @NotBlank(message = "El nombre es obligatorio.") String name,
            @Email(message = "El correo no tiene un formato valido.") String email) {
    }

}

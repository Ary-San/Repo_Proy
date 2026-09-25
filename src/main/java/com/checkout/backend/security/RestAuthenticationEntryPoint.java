package com.checkout.backend.security;

import com.checkout.backend.exceptions.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Responde 401 con el mismo cuerpo que el resto de la API.
 *
 * Existe por una limitacion concreta del @RestControllerAdvice: solo ve lo que
 * pasa por el DispatcherServlet. Un token invalido lo rechaza la cadena de
 * filtros, mucho antes, y sin esta clase ese 401 saldria con el formato de error
 * por defecto del contenedor. El cliente tendria que saber leer dos formas
 * distintas de error segun donde se rompio la peticion.
 *
 * Queda pendiente en el javadoc de GlobalExceptionHandler desde el issue #3;
 * esto lo cierra.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * El mensaje es generico a proposito. La excepcion distingue entre credencial
     * ausente, token caducado y firma invalida, y decirselo al cliente le dice a
     * quien prueba tokens cual de sus intentos se acerco.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        write(response, HttpStatus.UNAUTHORIZED,
                "Debes autenticarte para acceder a este recurso.",
                request.getRequestURI(), objectMapper);
    }

    /** Compartido con RestAccessDeniedHandler: los dos escriben el mismo DTO. */
    static void write(HttpServletResponse response, HttpStatus status, String message,
                      String path, ObjectMapper objectMapper) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponseDTO.of(status, message, path));
    }

}

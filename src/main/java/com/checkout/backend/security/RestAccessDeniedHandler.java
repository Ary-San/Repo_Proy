package com.checkout.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Responde 403 con el cuerpo estandar cuando el usuario esta autenticado pero no
 * tiene permiso.
 *
 * La pareja de RestAuthenticationEntryPoint: aquel cubre "no se quien eres",
 * este cubre "se quien eres y no te alcanza".
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        RestAuthenticationEntryPoint.write(response, HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta accion.",
                request.getRequestURI(), objectMapper);
    }

}

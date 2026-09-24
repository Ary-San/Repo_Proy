package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 403: el usuario esta autenticado pero no puede hacer esta operacion.
 *
 * Dos situaciones: le falta el rol (@PreAuthorize lo cubre solo, ver
 * GlobalExceptionHandler) o el recurso es de otro usuario. La segunda no la
 * puede resolver Spring Security con anotaciones, porque depende de comparar el
 * dueno de la fila con quien hace el pedido, y ese es justo el caso para el que
 * existe esta excepcion.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

}

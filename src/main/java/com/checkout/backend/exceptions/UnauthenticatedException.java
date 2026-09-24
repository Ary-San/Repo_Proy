package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 401: no hay credenciales validas, o el token vencio o esta mal firmado.
 *
 * 401 y 403 se confunden seguido y son distintos: 401 es "no se quien eres",
 * 403 es "se quien eres y no te alcanza". El cliente reacciona distinto en cada
 * caso, refrescando el token o mostrando un aviso de permisos.
 *
 * El mensaje debe quedarse en lo generico. Distinguir "el usuario no existe" de
 * "la contrasena es incorrecta" le regala a quien prueba credenciales la lista
 * de correos registrados.
 */
public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

}

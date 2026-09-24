package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 409: el recurso choca con uno que ya existe.
 *
 * El caso claro es registrarse con un correo ya tomado. Podria devolverse 400,
 * pero 409 dice algo mas preciso: el request esta bien formado, el problema es
 * el estado actual del servidor.
 *
 * Existe para que el service pueda revisar el duplicado antes de insertar y
 * responder con un mensaje entendible. Cuando la carrera gana la base de datos,
 * la DataIntegrityViolationException que sale del constraint UNIQUE termina en el
 * mismo 409 por el handler global, de modo que las dos rutas responden igual.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

}

package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 400: el request es sintacticamente valido pero viola una regla de negocio.
 *
 * Cubre lo que Bean Validation no puede ver porque depende del estado guardado:
 * aportar mas de lo que falta para una meta, vender un activo que no esta en la
 * cartera, registrar un gasto contra un presupuesto ya cerrado. Las anotaciones
 * del DTO revisan un campo aislado; esto revisa la operacion completa.
 */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

}

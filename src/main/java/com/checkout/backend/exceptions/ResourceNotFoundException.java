package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 404: se pidio un recurso que no existe.
 *
 * Es la excepcion que lanza un service cuando un findById vuelve vacio. Sin ella
 * el Optional se termina desempaquetando con orElseThrow() y sale un 500, que le
 * dice al cliente "el servidor se rompio" cuando en realidad el id estaba mal.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Atajo para el caso frecuente: recurso y identificador con el que se busco.
     *
     * <pre>new ResourceNotFoundException("Usuario", id)  ->  "Usuario no encontrado: 42"</pre>
     *
     * El identificador entra en el mensaje porque es un dato que el cliente ya
     * conoce: lo acaba de mandar en la URL.
     */
    public ResourceNotFoundException(String resource, Object identifier) {
        super(HttpStatus.NOT_FOUND, resource + " no encontrado: " + identifier);
    }

}

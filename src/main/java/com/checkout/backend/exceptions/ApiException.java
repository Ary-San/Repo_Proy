package com.checkout.backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Raiz de las excepciones propias de la aplicacion.
 *
 * Cada subclase fija el codigo HTTP con el que quiere salir, y el handler global
 * lo lee de aqui. La alternativa habitual es que el handler tenga un switch o un
 * Map<Class, HttpStatus> decidiendo el codigo de cada excepcion; eso parte la
 * informacion en dos archivos y hace que agregar una excepcion nueva sin tocar
 * el mapa devuelva 500 en silencio. Asi el codigo viaja con la excepcion y una
 * subclase nueva funciona sin modificar el handler.
 *
 * Es RuntimeException y no checked porque estas excepciones no se atrapan: se
 * lanzan en el service y se dejan subir hasta el handler. Obligar a declararlas
 * en cada firma intermedia no agrega ninguna garantia.
 *
 * El mensaje de una ApiException se devuelve tal cual al cliente, asi que no
 * debe contener detalles internos (queries, rutas de archivos, stack traces).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}

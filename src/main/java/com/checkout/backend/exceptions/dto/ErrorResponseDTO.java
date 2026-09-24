package com.checkout.backend.exceptions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Unica forma en la que la API responde un error.
 *
 * Es un record y no una clase con Lombok a proposito: una respuesta de error se
 * arma de una sola vez en el handler y nunca se vuelve a tocar, asi que no hay
 * nada que construir por partes ni ningun campo que un setter deba poder
 * cambiar despues.
 *
 * Los cinco campos que pide el contrato son timestamp, status, error, message y
 * path, y los cinco aparecen siempre. fieldErrors es un sexto campo opcional:
 * cuando el 400 viene de Bean Validation, decir solo "Validation failed" obliga
 * al cliente a adivinar que campo rechazo el servidor. Es el unico anotado con
 * @JsonInclude(NON_EMPTY), de modo que desaparece del JSON en los errores que no
 * son de validacion en vez de aparecer como "fieldErrors": null.
 *
 * @param timestamp  momento en que se genero la respuesta, hora del servidor
 * @param status     codigo HTTP numerico, p. ej. 404
 * @param error      frase de razon del codigo, p. ej. "Not Found"
 * @param message    descripcion legible, segura para mostrarle al usuario
 * @param path       URI que se pidio, sin el query string
 * @param fieldErrors detalle por campo cuando el error es de validacion
 */
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,

        /*
         * La anotacion va sobre este componente y no sobre el record entero. A
         * nivel de tipo aplica a los seis campos, y entonces un error cuyo
         * mensaje quedara vacio se serializaria sin la clave "message",
         * rompiendo el contrato de los cinco campos que siempre estan.
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorDTO> fieldErrors
) {

    /**
     * Un campo rechazado por Bean Validation.
     *
     * @param field   nombre del campo del request
     * @param message por que se rechazo
     */
    public record FieldErrorDTO(String field, String message) {
    }

    /**
     * Caso normal: un error sin detalle por campo.
     *
     * El timestamp lo pone este metodo y no quien llama, para que dos errores
     * emitidos por el mismo handler no puedan diferir en como se genero la hora.
     */
    public static ErrorResponseDTO of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    /**
     * Caso de validacion: el mismo error mas la lista de campos rechazados.
     */
    public static ErrorResponseDTO of(HttpStatus status, String message, String path,
                                      List<FieldErrorDTO> fieldErrors) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
    }

}

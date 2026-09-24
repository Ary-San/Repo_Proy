package com.checkout.backend.exceptions;

import com.checkout.backend.exceptions.dto.ErrorResponseDTO;
import com.checkout.backend.exceptions.dto.ErrorResponseDTO.FieldErrorDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el contrato del cuerpo de error a nivel de serializacion.
 *
 * Los tests de MockMvc comprueban cada caso de error por separado; esto
 * comprueba la regla que los atraviesa a todos: que los cinco campos del
 * contrato esten siempre y que fieldErrors aparezca solo cuando tiene algo.
 *
 * Existe por un defecto concreto. La anotacion @JsonInclude(NON_EMPTY) estaba
 * puesta sobre el record entero, y a nivel de tipo aplica a los seis campos: un
 * error cuyo mensaje quedara vacio se habria serializado sin la clave "message",
 * y el cliente que la leyera sin comprobar habria roto. Ahora la anotacion esta
 * solo sobre fieldErrors, y el primer test de abajo es el que lo sostiene.
 */
class ErrorResponseDTOTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Los cinco campos del contrato salen siempre, incluso si el mensaje queda vacio")
    void contractFieldsAreAlwaysPresent() {
        String json = mapper.writeValueAsString(
                ErrorResponseDTO.of(HttpStatus.BAD_REQUEST, "", "/api/v1/usuarios"));

        assertThat(json)
                .contains("\"timestamp\"")
                .contains("\"status\"")
                .contains("\"error\"")
                .contains("\"message\"")
                .contains("\"path\"");
    }

    @Test
    @DisplayName("fieldErrors no aparece cuando el error no es de validacion")
    void fieldErrorsIsOmittedWhenEmpty() {
        String json = mapper.writeValueAsString(
                ErrorResponseDTO.of(HttpStatus.NOT_FOUND, "Usuario no encontrado: 1", "/api/v1/usuarios/1"));

        assertThat(json).doesNotContain("fieldErrors");
    }

    @Test
    @DisplayName("fieldErrors aparece con su detalle cuando hay campos rechazados")
    void fieldErrorsIsIncludedWhenPresent() {
        String json = mapper.writeValueAsString(ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene campos invalidos.",
                "/api/v1/usuarios",
                List.of(new FieldErrorDTO("email", "El correo no tiene un formato valido."))));

        assertThat(json)
                .contains("\"fieldErrors\"")
                .contains("\"field\":\"email\"")
                .contains("El correo no tiene un formato valido.");
    }

    @Test
    @DisplayName("status y error se derivan del HttpStatus, no se pasan a mano")
    void statusAndReasonPhraseComeFromHttpStatus() {
        ErrorResponseDTO response = ErrorResponseDTO.of(
                HttpStatus.UNPROCESSABLE_ENTITY, "cualquiera", "/x");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.error()).isEqualTo("Unprocessable Entity");
        assertThat(response.timestamp()).isNotNull();
        // La sobrecarga corta no deja fieldErrors en null: una lista vacia se
        // omite igual al serializar y no obliga a nadie a comprobar null.
        assertThat(response.fieldErrors()).isEmpty();
    }

}

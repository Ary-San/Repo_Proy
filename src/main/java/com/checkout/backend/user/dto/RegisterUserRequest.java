package com.checkout.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public sign-up payload. The plain password lives only in this DTO; the entity stores the BCrypt hash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100,
            message = "The password must be at least 8 characters long")
    /*
     * Politica de complejidad: una minuscula, una mayuscula, un digito y un
     * caracter que no sea ninguno de los tres.
     *
     * La longitud se deja en @Size y no se mete en esta expresion a proposito.
     * Son dos reglas distintas y cada una tiene su mensaje: asi el cuerpo del
     * error dice si la contrasena es corta o si le falta variedad, en vez de
     * responder lo mismo en los dos casos y dejar al usuario adivinando.
     *
     * La comprobacion se hace con lookaheads porque el orden de los caracteres
     * no importa; exigir un patron posicional obligaria a escribir la clave en
     * una forma concreta, que no es lo que se quiere.
     *
     * El cuarto grupo se define por exclusion, [^A-Za-z0-9], y no como una lista
     * de simbolos permitidos: una lista deja fuera acentos, la enie y cualquier
     * simbolo que no se le haya ocurrido a quien la escribio, y rechazar una
     * contrasena mas fuerte por no estar en la lista es justo lo contrario de lo
     * que busca la regla.
     */
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "The password must contain an uppercase letter, "
                    + "a lowercase letter, a digit and a special character")
    private String password;

    @Past
    private LocalDate birthDate;

}

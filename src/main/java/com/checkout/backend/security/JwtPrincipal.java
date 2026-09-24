package com.checkout.backend.security;

import java.security.Principal;

/**
 * Identidad reconstruida a partir de un token, sin tocar la base de datos.
 *
 * Implementa Principal para que getName() del Authentication siga devolviendo el
 * correo. Sin eso, todo lo que identifique al usuario por su nombre — los logs
 * de Spring Security, las expresiones de @PreAuthorize que usan
 * authentication.name — pasaria a ver el toString del objeto.
 *
 * @param id    identificador del usuario, del claim uid
 * @param email correo, que es el subject del token
 */
public record JwtPrincipal(Long id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }

}

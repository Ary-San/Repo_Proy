package com.checkout.backend.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de los tokens, leida de variables de entorno.
 *
 * El secreto no tiene valor por defecto a proposito. Un default hace que la
 * aplicacion arranque en cualquier lado, incluido produccion, firmando con una
 * clave que esta en el repositorio: cualquiera que lea el codigo puede emitirse
 * un token valido. Sin default, el arranque falla con un mensaje claro y el
 * problema se descubre al desplegar, no despues de una filtracion.
 *
 * @param secret                  clave HMAC. Minimo 32 caracteres: HS256 exige
 *                                256 bits y jjwt rechaza una clave mas corta al
 *                                construirla, no al firmar
 * @param accessTokenExpiration   vida del token de acceso. Corta, porque viaja
 *                                en cada peticion y no se puede revocar
 * @param refreshTokenExpiration  vida del token de refresco. Larga, pero este si
 *                                se guarda en la base y se puede revocar
 */
@ConfigurationProperties(prefix = "checkout.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration
) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "checkout.jwt.secret debe tener al menos 32 caracteres. "
                            + "Se configura con la variable de entorno JWT_SECRET.");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isNegative()
                || accessTokenExpiration.isZero()) {
            throw new IllegalStateException("checkout.jwt.access-token-expiration debe ser positiva.");
        }
        if (refreshTokenExpiration == null || refreshTokenExpiration.isNegative()
                || refreshTokenExpiration.isZero()) {
            throw new IllegalStateException("checkout.jwt.refresh-token-expiration debe ser positiva.");
        }
    }

}

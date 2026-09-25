package com.checkout.backend.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de los tokens, leida de variables de entorno.
 *
 * El prefijo y los nombres siguen los que ya estaban en application.properties
 * antes de este cambio, para no tener dos convenciones conviviendo. Solo se
 * agrega expiration-refresh, que no existia porque tampoco habia refresh
 * tokens.
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
 * @param expirationAccess        vida del token de acceso. Corta, porque viaja
 *                                en cada peticion y no se puede revocar
 * @param expirationRefresh       vida del token de refresco. Larga, pero este si
 *                                se guarda en la base y se puede revocar
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Duration expirationAccess,
        Duration expirationRefresh
) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos 32 caracteres. "
                            + "Se configura con la variable de entorno JWT_SECRET.");
        }
        if (expirationAccess == null || expirationAccess.isNegative()
                || expirationAccess.isZero()) {
            throw new IllegalStateException("jwt.expiration-access debe ser positiva.");
        }
        if (expirationRefresh == null || expirationRefresh.isNegative()
                || expirationRefresh.isZero()) {
            throw new IllegalStateException("jwt.expiration-refresh debe ser positiva.");
        }
    }

}

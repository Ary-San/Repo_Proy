package com.checkout.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import com.checkout.backend.user.model.Role;
import com.checkout.backend.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emite y valida los tokens de acceso.
 *
 * El token lleva el correo como subject y los roles como claim. Poner los roles
 * dentro evita una consulta a la base en cada peticion, y el precio es que un
 * cambio de rol no se aplica hasta que el token expira: por eso la vida del
 * token de acceso es corta.
 *
 * Lo que el token NO lleva es nada sensible. Un JWT va firmado, no cifrado:
 * cualquiera que lo intercepte puede leer su contenido decodificando base64.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Nombre del claim con los roles. */
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // Lanza WeakKeyException si la clave no llega a 256 bits, de modo que
        // una clave corta rompe el arranque y no la firma de cada token.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Token de acceso para un usuario.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenExpiration());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(ROLES_CLAIM, user.getRoles().stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Segundos que le quedan de vida a un token recien emitido. Va en la
     * respuesta de login para que el cliente sepa cuando refrescar sin tener que
     * decodificar el token.
     */
    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenExpiration().toSeconds();
    }

    /**
     * Comprueba firma y vigencia, y devuelve los claims.
     *
     * Devuelve null en vez de lanzar porque quien llama es un filtro que se
     * ejecuta en cada peticion: un token invalido es un caso esperado, no una
     * anomalia, y propagar la excepcion obligaria a atraparla ahi mismo.
     *
     * El motivo del rechazo se registra en DEBUG y nunca llega al cliente: la
     * diferencia entre "firma invalida" y "token expirado" le dice a quien
     * prueba tokens si va por buen camino.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rechazado: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Roles guardados en el token.
     *
     * Un nombre de rol que ya no existe en el enum se descarta en lugar de
     * romper la peticion: puede venir de un token emitido antes de que el enum
     * cambiara, y el usuario no tiene forma de arreglarlo salvo volver a entrar.
     */
    public Set<Role> extractRoles(Claims claims) {
        Object raw = claims.get(ROLES_CLAIM);
        if (!(raw instanceof List<?> values)) {
            return Set.of();
        }

        return values.stream()
                .map(String::valueOf)
                .map(name -> {
                    try {
                        return Role.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        log.debug("Rol desconocido en el token: {}", name);
                        return null;
                    }
                })
                .filter(role -> role != null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

}

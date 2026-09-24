package com.checkout.backend.security;

import com.checkout.backend.user.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el token del encabezado Authorization y deja al usuario autenticado en el
 * contexto.
 *
 * Extiende OncePerRequestFilter porque un forward interno vuelve a pasar por la
 * cadena de filtros; sin esa garantia el trabajo se repetiria en cada salto.
 *
 * El filtro nunca rechaza una peticion. Si no hay token, o no vale, deja el
 * contexto vacio y sigue: quien decide si esa ruta necesitaba autenticacion es
 * la cadena de seguridad, no este filtro. Mezclar las dos cosas obligaria a
 * repetir aqui la lista de rutas publicas.
 *
 * No consulta la base: el correo y los roles salen del token ya firmado. Eso
 * evita un viaje a la base por peticion, con la contrapartida de que un cambio
 * de rol tarda en aplicarse lo que dure el token de acceso.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        // El segundo chequeo evita pisar una autenticacion que otro filtro ya
        // dejo puesta, por ejemplo en una peticion que vuelve por un forward.
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseToken(token);

            if (claims != null) {
                Set<Role> roles = tokenProvider.extractRoles(claims);

                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                claims.getSubject(), null, UserPrincipal.authoritiesOf(roles));
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Saca el token del encabezado.
     *
     * El prefijo se compara sin distinguir mayusculas porque el RFC 7235 declara
     * el esquema insensible a mayusculas, y algunos clientes mandan "bearer".
     */
    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);

        if (header == null || header.length() <= BEARER_PREFIX.length()
                || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

}

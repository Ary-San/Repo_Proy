package com.checkout.backend.security;

import com.checkout.backend.user.model.User;
import com.checkout.backend.user.model.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapta la entidad User a lo que Spring Security entiende.
 *
 * Envuelve la entidad en vez de copiar sus campos para que el resto del codigo
 * pueda recuperarla del principal sin volver a consultarla.
 *
 * Los estados del usuario se traducen a los dos metodos que Spring ya conoce, y
 * la diferencia importa: un usuario BLOCKED da "cuenta bloqueada" y uno INACTIVE
 * da "cuenta deshabilitada". Devolver siempre false en isEnabled perderia esa
 * distincion y el cliente no podria decir si conviene reactivar o contactar
 * soporte.
 */
public class UserPrincipal implements UserDetails {

    /**
     * Prefijo que Spring Security espera en una autoridad para tratarla como
     * rol. Sin el, hasRole('USER') no encuentra nada aunque la autoridad se
     * llame USER.
     */
    private static final String ROLE_PREFIX = "ROLE_";

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /** El correo es el nombre de usuario: es lo unico con constraint UNIQUE. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.BLOCKED;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    /**
     * No hay caducidad de cuentas ni de contrasenas en el modelo, asi que estos
     * dos siempre son true. Se declaran igual para que quede escrito que es una
     * decision y no un olvido.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Autoridades de una lista de roles, para construir el principal desde un token. */
    public static List<GrantedAuthority> authoritiesOf(Collection<com.checkout.backend.user.model.Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

}

package com.checkout.backend.security;

import com.checkout.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga al usuario para el AuthenticationManager.
 *
 * Es lo que usa el login: recibe el correo, trae la fila y deja que Spring
 * compare el hash. La comparacion no se hace aqui a proposito, porque el
 * AuthenticationProvider ya la hace en tiempo constante.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * El mensaje de la excepcion dice que el correo no existe, y eso esta bien
     * aqui: Spring la convierte en BadCredentialsException antes de que llegue a
     * ninguna respuesta, de modo que el cliente recibe un 401 generico. El
     * detalle solo vive en el log.
     *
     * Los roles son una @ElementCollection EAGER, asi que vienen cargados y el
     * principal no depende de la sesion de Hibernate.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No hay ningun usuario con el correo " + email));
    }

}

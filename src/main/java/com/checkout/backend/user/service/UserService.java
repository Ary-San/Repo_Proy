package com.checkout.backend.user.service;

import com.checkout.backend.token_wallet.refresh_token.service.RefreshTokenService;
import com.checkout.backend.user.dto.UpdateUserRequest;
import com.checkout.backend.user.dto.UserResponse;
import com.checkout.backend.user.model.User;
import com.checkout.backend.user.model.UserStatus;
import com.checkout.backend.user.repository.UserRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta y mantenimiento de la cuenta.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final ModelMapper mapper;

    public UserService(UserRepository userRepository,
                       RefreshTokenService refreshTokenService,
                       ModelMapper mapper) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.mapper = mapper;
    }

    public UserResponse toResponse(User user) {
        return mapper.map(user, UserResponse.class);
    }

    /**
     * Actualiza los campos que el usuario puede cambiar de si mismo.
     *
     * UpdateUserRequest no incluye correo, roles ni estado, y eso es lo que
     * impide que alguien se ascienda a ADMIN con un campo de mas en el JSON. Los
     * dos campos son opcionales: un null significa "no lo toques", no "borralo".
     */
    @Transactional
    public UserResponse updateProfile(User user, UpdateUserRequest request) {
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        return toResponse(userRepository.save(user));
    }

    /**
     * Da de baja la cuenta sin borrarla.
     *
     * Es baja logica porque la fila esta referenciada por ahorros, metas,
     * ingresos y gastos: borrarla destruiria el historial financiero o romperia
     * las claves foraneas. El correo sigue siendo unico entre todos los estados,
     * de modo que volver es reactivar y no registrarse otra vez.
     *
     * Revocar los tokens es parte de la baja: sin eso la cuenta queda inactiva
     * pero cualquier refresh token emitido seguiria sirviendo para renovar.
     */
    @Transactional
    public void deactivate(User user) {
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    /** Listado completo. Solo para administracion; ver el control en el controller. */
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

}

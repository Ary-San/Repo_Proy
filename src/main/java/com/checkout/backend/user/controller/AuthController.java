package com.checkout.backend.user.controller;

import com.checkout.backend.user.dto.AuthResponse;
import com.checkout.backend.user.dto.LoginRequest;
import com.checkout.backend.user.dto.RefreshTokenRequest;
import com.checkout.backend.user.dto.RegisterUserRequest;
import com.checkout.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrada y salida de la aplicacion.
 *
 * Es el unico controller con rutas publicas, declaradas en SecurityConfig. Son
 * las que un usuario sin sesion necesita para conseguir una; todo lo demas exige
 * token.
 *
 * /auth no nombra un recurso sino una accion, y eso se aparta de la convencion
 * que sigue el resto de la API. Es deliberado: iniciar sesion no crea ni
 * modifica una entidad, y forzarlo a parecer un recurso — POST /sessions —
 * describiria peor lo que pasa de lo que ayudaria la coherencia.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/v1/auth/register
     *
     * 201 porque crea un usuario. No lleva Location: el recurso creado solo es
     * accesible en /users/me, que no depende de un id, y el cuerpo ya trae al
     * usuario junto con los tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /api/v1/auth/login
     *
     * 200 y no 201: no crea nada del lado del servidor. La sesion vive en el
     * token que se lleva el cliente.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** POST /api/v1/auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * POST /api/v1/auth/logout
     *
     * 204 sin cuerpo. Responde igual aunque el token no exista: decir lo
     * contrario permitiria averiguar que tokens son validos sin tener ninguno.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

}

package com.checkout.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Built once by the authentication service and never mutated afterwards, so it
 * exposes no setter. It is assembled by hand rather than mapped, because none
 * of its fields come from a single entity.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private UserResponse user;

}

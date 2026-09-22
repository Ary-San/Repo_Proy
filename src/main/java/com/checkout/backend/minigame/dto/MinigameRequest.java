package com.checkout.backend.minigame.dto;

import com.checkout.backend.minigame.model.MinigameStatus;
import com.checkout.backend.minigame.model.MinigameType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Admin-only payload for the minigame catalogue.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotNull
    private MinigameType type;

    @Size(max = 120)
    private String topic;

    @NotNull
    @DecimalMin("0")
    private BigDecimal tokenCost;

    @NotNull
    @DecimalMin("0")
    private BigDecimal maxTokenReward;

    @NotNull
    private MinigameStatus status;

}

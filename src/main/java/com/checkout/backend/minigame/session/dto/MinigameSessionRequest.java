package com.checkout.backend.minigame.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameSessionRequest {

    @NotNull
    private Long minigameId;

    @NotNull
    @Min(0)
    private Integer score;

}

package com.checkout.backend.minigame.session.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class MinigameSessionResponse {

    private Long id;

    private String minigameTitle;

    private Integer score;

    private BigDecimal tokensEarned;

    private LocalDateTime playedAt;

}

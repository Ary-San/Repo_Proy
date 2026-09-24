package com.checkout.backend.savings.dto;

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
public class SavingsResponse {

    private Long id;

    private BigDecimal currentBalance;

    /**
     * Part of the balance already committed to goals still in progress.
     * Computed by the service: a goal is an envelope over this same money, so
     * currentBalance already contains it.
     */
    private BigDecimal committedAmount;

    /** currentBalance minus committedAmount. Computed by the service. */
    private BigDecimal availableBalance;

    private LocalDateTime updatedAt;

}

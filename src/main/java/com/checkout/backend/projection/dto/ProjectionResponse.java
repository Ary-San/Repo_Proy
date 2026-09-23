package com.checkout.backend.projection.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Carries both results plus their difference: the comparison is the point.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectionResponse {

    private Long id;

    private String name;

    private BigDecimal initialCapital;

    private BigDecimal monthlyContribution;

    private BigDecimal annualRate;

    private Integer periods;

    private BigDecimal totalContributed;

    private BigDecimal simpleFinalAmount;

    private BigDecimal compoundFinalAmount;

    private BigDecimal difference;

    private LocalDateTime calculatedAt;

}

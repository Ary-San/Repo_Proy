package com.checkout.backend.projection.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * annualRate is the TEA expressed between 0 and 1 (0.075 for 7.5%).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectionRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @DecimalMin("0")
    private BigDecimal initialCapital;

    @NotNull
    @DecimalMin("0")
    private BigDecimal monthlyContribution;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal annualRate;

    @NotNull
    @Min(1)
    @Max(600)
    private Integer periods;

}

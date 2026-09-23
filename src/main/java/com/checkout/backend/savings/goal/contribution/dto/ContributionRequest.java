package com.checkout.backend.savings.goal.contribution.dto;

import com.checkout.backend.savings.goal.contribution.model.ContributionSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class ContributionRequest {

    @NotNull
    @DecimalMin(value = "0.01",
            message = "The contribution must be greater than zero")
    private BigDecimal amount;

    @NotNull
    private ContributionSource source;

}

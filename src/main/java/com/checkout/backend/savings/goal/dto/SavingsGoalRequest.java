package com.checkout.backend.savings.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class SavingsGoalRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @DecimalMin(value = "0.01",
            message = "The target amount must be greater than zero")
    private BigDecimal targetAmount;

    @NotNull
    @Future
    private LocalDate deadline;

}

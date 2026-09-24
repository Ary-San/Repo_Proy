package com.checkout.backend.savings.income.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class IncomeRequest {

    @NotNull
    @DecimalMin(value = "0.01",
            message = "The amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 100)
    private String source;

    @NotNull
    @PastOrPresent(message = "The date cannot be in the future")
    private LocalDate date;

    @Size(max = 255)
    private String description;

}

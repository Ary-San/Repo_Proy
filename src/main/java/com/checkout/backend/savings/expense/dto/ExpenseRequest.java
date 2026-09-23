package com.checkout.backend.savings.expense.dto;

import com.checkout.backend.savings.expense.model.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
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
public class ExpenseRequest {

    @NotNull
    private ExpenseCategory category;

    @NotNull
    @DecimalMin(value = "0.01",
            message = "The amount must be greater than zero")
    private BigDecimal amount;

    @NotNull
    private LocalDate date;

    @Size(max = 255)
    private String description;

}

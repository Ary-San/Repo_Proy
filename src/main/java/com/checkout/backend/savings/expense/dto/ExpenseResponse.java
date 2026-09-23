package com.checkout.backend.savings.expense.dto;

import com.checkout.backend.savings.expense.model.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ExpenseResponse {

    private Long id;

    private ExpenseCategory category;

    private BigDecimal amount;

    private LocalDate date;

    private String description;

    private LocalDateTime createdAt;

}

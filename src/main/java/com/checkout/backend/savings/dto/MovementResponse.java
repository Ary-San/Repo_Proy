package com.checkout.backend.savings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified feed of incomes and expenses. There is no Movement table: both entities map into this shape in the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovementResponse {

    private Long id;

    private String kind;

    private String label;

    private String category;

    private BigDecimal amount;

    private LocalDate date;

}

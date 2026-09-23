package com.checkout.backend.savings.goal.contribution.dto;

import com.checkout.backend.savings.goal.contribution.model.ContributionSource;
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
public class ContributionResponse {

    private Long id;

    private BigDecimal amount;

    private ContributionSource source;

    private LocalDateTime createdAt;

}

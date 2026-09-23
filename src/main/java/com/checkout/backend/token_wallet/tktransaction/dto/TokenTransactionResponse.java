package com.checkout.backend.token_wallet.tktransaction.dto;

import com.checkout.backend.token_wallet.tktransaction.model.TokenReason;
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
public class TokenTransactionResponse {

    private Long id;

    private BigDecimal amount;

    private TokenReason reason;

    private Long referenceId;

    private LocalDateTime createdAt;

}

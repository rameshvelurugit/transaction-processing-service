package com.transactionprocessing.transaction.dto;

import com.transactionprocessing.common.constants.TransactionConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "requestId is required")
    private String requestId;

    @NotNull(message = "sequenceNumber is required")
    @Positive(message = "sequenceNumber must be positive")
    private Integer sequenceNumber;

    @NotBlank(message = "accountId is required")
    @Pattern(regexp = TransactionConstants.ACCOUNT_ID_PATTERN, message = "accountId must match ACC-####")
    private String accountId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "type is required")
    @Pattern(regexp = TransactionConstants.TYPE_PATTERN, message = "type must be DEBIT or CREDIT")
    private String type;
}

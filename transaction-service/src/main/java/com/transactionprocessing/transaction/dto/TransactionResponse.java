package com.transactionprocessing.transaction.dto;

import com.transactionprocessing.transaction.entity.TransactionStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class TransactionResponse {

    Long id;
    String transactionId;
    String requestId;
    Integer sequenceNumber;
    String accountId;
    BigDecimal amount;
    String type;
    TransactionStatus status;
    int retryCount;
    String errorMessage;
    Instant createdAt;
    Instant updatedAt;
    Instant processedAt;
}

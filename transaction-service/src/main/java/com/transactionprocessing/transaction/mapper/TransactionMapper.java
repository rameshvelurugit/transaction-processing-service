package com.transactionprocessing.transaction.mapper;

import com.transactionprocessing.transaction.dto.TransactionResponse;
import com.transactionprocessing.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .requestId(transaction.getRequestId())
                .sequenceNumber(transaction.getSequenceNumber())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .retryCount(transaction.getRetryCount())
                .errorMessage(transaction.getErrorMessage())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .processedAt(transaction.getProcessedAt())
                .build();
    }
}

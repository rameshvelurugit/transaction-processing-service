package com.transactionprocessing.transaction.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Bean Validation constraints on {@link TransactionRequest} field values and formats.
 */
class TransactionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestPassesValidation() {
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .sequenceNumber(1)
                .accountId("ACC-1001")
                .amount(new BigDecimal("10.00"))
                .type("CREDIT")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invalidAccountAndTypeFailValidation() {
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .sequenceNumber(1)
                .accountId("INVALID")
                .amount(new BigDecimal("10.00"))
                .type("TRANSFER")
                .build();

        Set<?> violations = validator.validate(request);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }
}

package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.constants.TransactionConstants;
import com.transactionprocessing.common.exception.RetryableException;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private RetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new RetryService(transactionRepository, new SimpleMeterRegistry());
    }

    @Test
    void marksRetryPendingBelowMaxAttempts() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .retryCount(0)
                .status(TransactionStatus.PROCESSING)
                .build();

        retryService.handleRetryableFailure(txn, new RetryableException("timeout"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TransactionStatus.RETRY_PENDING);
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
    }

    @Test
    void marksFailedWhenMaxRetriesExceeded() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .retryCount(TransactionConstants.MAX_RETRY_ATTEMPTS)
                .status(TransactionStatus.PROCESSING)
                .build();

        retryService.handleRetryableFailure(txn, new RetryableException("timeout"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
    }
}

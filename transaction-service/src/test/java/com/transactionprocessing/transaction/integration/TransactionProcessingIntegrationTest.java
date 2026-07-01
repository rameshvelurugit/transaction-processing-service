package com.transactionprocessing.transaction.integration;

import com.transactionprocessing.transaction.dto.TransactionRequest;
import com.transactionprocessing.transaction.entity.Account;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.AccountRepository;
import com.transactionprocessing.transaction.repository.ProcessedRequestRepository;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import com.transactionprocessing.transaction.service.TransactionProcessingService;
import com.transactionprocessing.transaction.service.TransactionService;
import com.transactionprocessing.transaction.util.ProcessingSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests covering the full transaction lifecycle with a real database.
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
@Sql(scripts = "/integration-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionProcessingIntegrationTest {

  private static final String ACCOUNT_ID = "ACC-1001";

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private TransactionProcessingService transactionProcessingService;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private ProcessedRequestRepository processedRequestRepository;

  @BeforeEach
  void seedAccount() {
    accountRepository.save(Account.builder()
        .accountId(ACCOUNT_ID)
        .balance(new BigDecimal("1000.00"))
        .build());
  }

  @Test
  void duplicateSubmissionDoesNotUpdateBalanceTwice() {
    TransactionRequest request = creditRequest("TXN-IDEM-1", "REQ-IDEM-1", 1, "50.00");

    transactionService.submit(request);
    transactionProcessingService.processPendingTransactions();

    BigDecimal balanceAfterFirst = accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance();
    assertThat(balanceAfterFirst).isEqualByComparingTo("1050.00");
    assertThat(processedRequestRepository.count()).isEqualTo(1);

    var duplicateResponse = transactionService.submit(request);
    assertThat(duplicateResponse.getStatus()).isEqualTo(TransactionStatus.DUPLICATE);

    transactionProcessingService.processPendingTransactions();

    assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance())
        .isEqualByComparingTo("1050.00");
    assertThat(processedRequestRepository.count()).isEqualTo(1);
    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-IDEM-1", "REQ-IDEM-1"))
        .isPresent();
  }

  @Test
  void insufficientFundsMarksTransactionFailedWithoutChangingBalance() {
    transactionService.submit(debitRequest("TXN-FAIL-1", "REQ-FAIL-1", 1, "5000.00"));

    transactionProcessingService.processPendingTransactions();

    var txn = transactionRepository.findByTransactionIdAndRequestId("TXN-FAIL-1", "REQ-FAIL-1")
        .orElseThrow();
    assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
    assertThat(txn.getErrorMessage()).contains("Insufficient funds");
    assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance())
        .isEqualByComparingTo("1000.00");
    assertThat(processedRequestRepository.count()).isZero();
  }

  @Test
  void outOfOrderSequenceProcessesPendingAfterEarlierSequenceCompletes() {
    transactionService.submit(debitRequest("TXN-OO-2", "REQ-OO-2", 2, "50.00"));
    transactionProcessingService.processPendingTransactions();

    var seq2 = transactionRepository.findByTransactionIdAndRequestId("TXN-OO-2", "REQ-OO-2")
        .orElseThrow();
    assertThat(seq2.getStatus()).isEqualTo(TransactionStatus.PENDING);

    transactionService.submit(creditRequest("TXN-OO-1", "REQ-OO-1", 1, "100.00"));
    transactionProcessingService.processPendingTransactions();

    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-OO-1", "REQ-OO-1")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-OO-2", "REQ-OO-2")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance())
        .isEqualByComparingTo("1050.00");
  }

  @Test
  void transientFailureRetriesThenSucceedsWithoutDuplicateIdempotencyRecord() {
    transactionService.submit(creditRequest(
        "TXN-RETRY-1",
        "REQ-RETRY-1",
        1,
        ProcessingSimulator.TRANSIENT_FAILURE_AMOUNT.toPlainString()));

    transactionProcessingService.processPendingTransactions();
    transactionProcessingService.processPendingTransactions();
    transactionProcessingService.processPendingTransactions();

    var retryPending = transactionRepository.findByTransactionIdAndRequestId("TXN-RETRY-1", "REQ-RETRY-1")
        .orElseThrow();
    assertThat(retryPending.getStatus()).isEqualTo(TransactionStatus.RETRY_PENDING);
    assertThat(retryPending.getRetryCount()).isEqualTo(3);

    transactionProcessingService.processPendingTransactions();

    var processed = transactionRepository.findByTransactionIdAndRequestId("TXN-RETRY-1", "REQ-RETRY-1")
        .orElseThrow();
    assertThat(processed.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    assertThat(processedRequestRepository.count()).isEqualTo(1);
    assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance())
        .isEqualByComparingTo("1999.99");
  }

  @Test
  void retryEndpointProcessesRetryPendingTransactions() {
    transactionService.submit(creditRequest(
        "TXN-API-RETRY",
        "REQ-API-RETRY",
        1,
        ProcessingSimulator.TRANSIENT_FAILURE_AMOUNT.toPlainString()));

    transactionProcessingService.processPendingTransactions();
    transactionProcessingService.processPendingTransactions();
    transactionProcessingService.processPendingTransactions();

    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-API-RETRY", "REQ-API-RETRY")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.RETRY_PENDING);

    var retryResponse = transactionService.retryFailed();

    assertThat(retryResponse.getRetriedCount()).isGreaterThanOrEqualTo(1);
    assertThat(retryResponse.getSuccessCount()).isGreaterThanOrEqualTo(1);
    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-API-RETRY", "REQ-API-RETRY")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.PROCESSED);
  }

  @Test
  void batchProcessingHandlesMultipleReceivedTransactions() {
    transactionService.submit(creditRequest("TXN-BATCH-1", "REQ-BATCH-1", 1, "10.00"));
    transactionService.submit(creditRequest("TXN-BATCH-2", "REQ-BATCH-2", 2, "20.00"));

    int processed = transactionProcessingService.processPendingTransactions();

    assertThat(processed).isGreaterThanOrEqualTo(2);
    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-BATCH-1", "REQ-BATCH-1")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    assertThat(transactionRepository.findByTransactionIdAndRequestId("TXN-BATCH-2", "REQ-BATCH-2")
        .orElseThrow().getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().getBalance())
        .isEqualByComparingTo("1030.00");
  }

  private TransactionRequest creditRequest(String transactionId, String requestId, int sequence, String amount) {
    return baseRequest(transactionId, requestId, sequence, amount, "CREDIT");
  }

  private TransactionRequest debitRequest(String transactionId, String requestId, int sequence, String amount) {
    return baseRequest(transactionId, requestId, sequence, amount, "DEBIT");
  }

  private TransactionRequest baseRequest(
      String transactionId, String requestId, int sequence, String amount, String type) {
    return TransactionRequest.builder()
        .transactionId(transactionId)
        .requestId(requestId)
        .sequenceNumber(sequence)
        .accountId(ACCOUNT_ID)
        .amount(new BigDecimal(amount))
        .type(type)
        .build();
  }
}

package com.transactionprocessing.transaction.repository;

import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void savesAndFindsByBusinessKeys() {
        Transaction txn = Transaction.builder()
                .transactionId("TXN-R1")
                .requestId("REQ-R1")
                .sequenceNumber(1)
                .accountId("ACC-1001")
                .amount(new BigDecimal("15.00"))
                .type("CREDIT")
                .status(TransactionStatus.RECEIVED)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        Optional<Transaction> found = transactionRepository.findByTransactionIdAndRequestId("TXN-R1", "REQ-R1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void countsByStatus() {
        transactionRepository.save(sample("TXN-A", "REQ-A"));
        transactionRepository.save(sample("TXN-B", "REQ-B"));

        assertThat(transactionRepository.countByStatus(TransactionStatus.RECEIVED)).isGreaterThanOrEqualTo(2);
    }

    private Transaction sample(String txnId, String reqId) {
        return Transaction.builder()
                .transactionId(txnId)
                .requestId(reqId)
                .sequenceNumber(1)
                .accountId("ACC-1001")
                .amount(new BigDecimal("1.00"))
                .type("CREDIT")
                .status(TransactionStatus.RECEIVED)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

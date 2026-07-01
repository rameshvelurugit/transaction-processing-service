package com.transactionprocessing.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the Spring application context loads successfully with the scheduler disabled for tests.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "transaction.processing.scheduler.enabled=false",
        "spring.sql.init.mode=never"
})
class TransactionServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}

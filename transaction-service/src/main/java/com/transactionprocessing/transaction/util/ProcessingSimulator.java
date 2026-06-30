package com.transactionprocessing.transaction.util;

import com.transactionprocessing.common.constants.TransactionConstants;

import java.math.BigDecimal;

public final class ProcessingSimulator {

    /** Amount used in sample data to simulate transient processing failures. */
    public static final BigDecimal TRANSIENT_FAILURE_AMOUNT = new BigDecimal("999.99");

    private ProcessingSimulator() {
    }

    public static boolean isTransientFailureAmount(BigDecimal amount) {
        return amount != null && TRANSIENT_FAILURE_AMOUNT.compareTo(amount) == 0;
    }

    public static boolean isInsufficientFunds(BigDecimal balance, String type, BigDecimal amount) {
        return TransactionConstants.TYPE_DEBIT.equals(type)
                && balance.compareTo(amount) < 0;
    }
}

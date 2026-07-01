package com.transactionprocessing.common.constants;

/**
 * Business rules and validation patterns governing transaction type, account format, and retry limits.
 */
public final class TransactionConstants {

    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final String TYPE_DEBIT = "DEBIT";
    public static final String TYPE_CREDIT = "CREDIT";
    public static final String ACCOUNT_ID_PATTERN = "ACC-\\d{4}";
    public static final String TYPE_PATTERN = "DEBIT|CREDIT";

    private TransactionConstants() {
    }
}

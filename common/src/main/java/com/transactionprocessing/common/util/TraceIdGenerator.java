package com.transactionprocessing.common.util;

import java.util.UUID;

/**
 * Generates compact 16-character trace identifiers for distributed request tracing.
 */
public final class TraceIdGenerator {

    private TraceIdGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

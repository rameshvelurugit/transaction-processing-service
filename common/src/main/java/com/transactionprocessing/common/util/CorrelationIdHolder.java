package com.transactionprocessing.common.util;

import com.transactionprocessing.common.logging.MdcContext;

/**
 * Provides convenient access to the current request's correlation ID from the MDC context.
 */
public final class CorrelationIdHolder {

    private CorrelationIdHolder() {
    }

    public static String current() {
        return MdcContext.getOrGenerateCorrelationId();
    }
}

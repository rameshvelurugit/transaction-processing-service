package com.transactionprocessing.common.util;

import com.transactionprocessing.common.logging.MdcContext;

public final class CorrelationIdHolder {

    private CorrelationIdHolder() {
    }

    public static String current() {
        return MdcContext.getOrGenerateCorrelationId();
    }
}

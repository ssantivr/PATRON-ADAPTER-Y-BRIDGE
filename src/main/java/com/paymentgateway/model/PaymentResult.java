package com.paymentgateway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outcome of a charge, returned by every {@code PaymentGatewayAPI} implementation.
 */
public record PaymentResult(boolean success,
                            String transactionId,
                            String provider,
                            BigDecimal amount,
                            BigDecimal fee,
                            String currency,
                            String message,
                            LocalDateTime processedAt) {

    public static PaymentResult approved(String transactionId, String provider,
                                         BigDecimal amount, BigDecimal fee, String currency) {
        return new PaymentResult(true, transactionId, provider, amount, fee, currency,
                "Payment approved.", LocalDateTime.now());
    }

    public static PaymentResult declined(String provider, BigDecimal amount, String currency, String reason) {
        return new PaymentResult(false, "-", provider, amount, BigDecimal.ZERO.setScale(2), currency,
                reason, LocalDateTime.now());
    }

    /** Amount the merchant actually receives after the gateway fee. */
    public BigDecimal netAmount() {
        return amount.subtract(fee);
    }
}

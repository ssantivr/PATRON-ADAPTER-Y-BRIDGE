package com.paymentgateway.model;

import java.math.BigDecimal;

/**
 * Data needed to issue an invoice through a {@code ModernInvoiceProvider}.
 */
public record InvoiceRequest(String customerName,
                             String customerEmail,
                             BigDecimal amount,
                             String currency,
                             String description,
                             String paymentReference) {

    /** Builds an invoice request for a payment that was already charged. */
    public static InvoiceRequest fromPayment(PaymentRequest payment, PaymentResult result) {
        return new InvoiceRequest(payment.customerName(), payment.customerEmail(), payment.amount(),
                payment.currency(), payment.description(), result.transactionId());
    }
}

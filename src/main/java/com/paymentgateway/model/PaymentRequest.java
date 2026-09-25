package com.paymentgateway.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable payment data that a view sends to a payment gateway.
 * <p>
 * Basic validation lives here so every gateway receives consistent, clean data.
 */
public record PaymentRequest(String customerName,
                             String customerEmail,
                             BigDecimal amount,
                             String currency,
                             String description) {

    private static final String EMAIL_PATTERN = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";

    public PaymentRequest {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (customerEmail == null || !customerEmail.trim().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
        customerName = customerName.trim();
        customerEmail = customerEmail.trim();
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        description = description == null ? "" : description.trim();
    }
}

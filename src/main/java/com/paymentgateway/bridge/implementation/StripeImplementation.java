package com.paymentgateway.bridge.implementation;

import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * BRIDGE PATTERN - Concrete Implementor.
 * <p>
 * Simulated Stripe API. Fees: 2.9% + 0.30 per charge.
 * Test rule: any amount ending in <b>.13</b> is declined by the "issuing bank".
 */
public class StripeImplementation implements PaymentGatewayAPI {

    private static final String PROVIDER_NAME = "Stripe";
    private static final BigDecimal PERCENTAGE_FEE = new BigDecimal("0.029");
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("50000.00");
    private static final BigDecimal DECLINE_TRIGGER_CENTS = new BigDecimal("0.13");
    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "GBP", "MXN");

    private final long simulatedLatencyMillis;

    public StripeImplementation() {
        this(700);
    }

    /** @param simulatedLatencyMillis artificial network delay (use 0 in tests) */
    public StripeImplementation(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(PERCENTAGE_FEE).add(FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        simulateNetworkCall();

        if (!SUPPORTED_CURRENCIES.contains(request.currency())) {
            return PaymentResult.declined(PROVIDER_NAME, request.amount(), request.currency(),
                    "Stripe does not support currency " + request.currency() + ".");
        }
        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            return PaymentResult.declined(PROVIDER_NAME, request.amount(), request.currency(),
                    "Amount exceeds Stripe's single-charge limit of 50,000.00.");
        }
        if (request.amount().remainder(BigDecimal.ONE).compareTo(DECLINE_TRIGGER_CENTS) == 0) {
            return PaymentResult.declined(PROVIDER_NAME, request.amount(), request.currency(),
                    "Card declined by the issuing bank (code: card_declined).");
        }

        String transactionId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return PaymentResult.approved(transactionId, PROVIDER_NAME, request.amount(),
                calculateFee(request.amount()), request.currency());
    }

    private void simulateNetworkCall() {
        if (simulatedLatencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME;
    }
}

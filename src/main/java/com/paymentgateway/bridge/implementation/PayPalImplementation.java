package com.paymentgateway.bridge.implementation;

import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * BRIDGE PATTERN - Concrete Implementor.
 * <p>
 * Simulated PayPal API. Fees: 3.49% + 0.49 per charge.
 * Test rule: any email containing <b>"blocked"</b> belongs to a restricted PayPal account.
 */
public class PayPalImplementation implements PaymentGatewayAPI {

    private static final String PROVIDER_NAME = "PayPal";
    private static final BigDecimal PERCENTAGE_FEE = new BigDecimal("0.0349");
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.49");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000.00");
    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "GBP", "CAD");

    private final long simulatedLatencyMillis;

    public PayPalImplementation() {
        this(900);
    }

    /** @param simulatedLatencyMillis artificial network delay (use 0 in tests) */
    public PayPalImplementation(long simulatedLatencyMillis) {
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
                    "PayPal does not support currency " + request.currency() + ".");
        }
        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            return PaymentResult.declined(PROVIDER_NAME, request.amount(), request.currency(),
                    "Amount exceeds PayPal's per-transaction limit of 10,000.00.");
        }
        if (request.customerEmail().toLowerCase(Locale.ROOT).contains("blocked")) {
            return PaymentResult.declined(PROVIDER_NAME, request.amount(), request.currency(),
                    "PayPal account is restricted (code: PAYER_ACCOUNT_RESTRICTED).");
        }

        String transactionId = "PAYID-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase(Locale.ROOT);
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

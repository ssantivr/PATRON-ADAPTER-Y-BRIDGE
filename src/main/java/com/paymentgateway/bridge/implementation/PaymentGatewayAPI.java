package com.paymentgateway.bridge.implementation;

import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * BRIDGE PATTERN - Implementor.
 * <p>
 * Low-level payment operations that every payment provider must offer.
 * The UI abstractions ({@code PaymentView} and its subclasses) only depend on this
 * interface, so new providers can be added without touching any view, and new views
 * can be added without touching any provider.
 */
public interface PaymentGatewayAPI {

    /** Human-readable provider name, e.g. "Stripe". */
    String getProviderName();

    /** ISO 4217 currency codes this provider accepts. */
    List<String> getSupportedCurrencies();

    /** Fee the provider charges for the given amount. */
    BigDecimal calculateFee(BigDecimal amount);

    /** Charges the customer. Never throws for business errors; returns a declined result instead. */
    PaymentResult charge(PaymentRequest request);
}

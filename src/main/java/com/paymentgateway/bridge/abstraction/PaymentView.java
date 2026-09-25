package com.paymentgateway.bridge.abstraction;

import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;

import javax.swing.JPanel;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * BRIDGE PATTERN - Abstraction.
 * <p>
 * Base class for every high-level payment screen. It holds a reference (the "bridge")
 * to a {@link PaymentGatewayAPI} implementation and delegates all payment work to it.
 * <p>
 * Views and gateways vary independently:
 * <pre>
 *   PaymentView ──────────────&gt; PaymentGatewayAPI
 *     ├─ PaymentDashboard          ├─ StripeImplementation
 *     └─ AdvancedInvoiceView       └─ PayPalImplementation
 * </pre>
 * 2 views x 2 gateways = 4 combinations with only 4 classes (not 4 subclasses per combination).
 */
public abstract class PaymentView extends JPanel {

    /** The bridge. Volatile because payments run on a background thread. */
    protected volatile PaymentGatewayAPI gateway;

    protected PaymentView(PaymentGatewayAPI gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    /** Tab / window title for this view. */
    public abstract String getViewTitle();

    /** Swaps the implementation at runtime; the view itself stays the same. */
    public final void setGateway(PaymentGatewayAPI newGateway) {
        this.gateway = Objects.requireNonNull(newGateway, "gateway");
        onGatewayChanged();
    }

    public PaymentGatewayAPI getGateway() {
        return gateway;
    }

    /** Hook for subclasses to refresh gateway-dependent UI (currencies, fees, labels...). */
    protected void onGatewayChanged() {
    }

    /** High-level operation implemented by delegating to the implementor. */
    protected PaymentResult processPayment(PaymentRequest request) {
        return gateway.charge(request);
    }

    protected static String formatMoney(BigDecimal amount, String currency) {
        return String.format(Locale.US, "%,.2f %s", amount, currency == null ? "" : currency);
    }
}

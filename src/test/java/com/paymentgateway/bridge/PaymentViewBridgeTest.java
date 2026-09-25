package com.paymentgateway.bridge;

import com.paymentgateway.bridge.abstraction.PaymentView;
import com.paymentgateway.bridge.implementation.PayPalImplementation;
import com.paymentgateway.bridge.implementation.StripeImplementation;
import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the core Bridge idea: the same abstraction object delegates to whichever
 * implementation it currently holds, and the implementation can be swapped at runtime.
 */
class PaymentViewBridgeTest {

    /** Minimal refined abstraction used only for testing. */
    private static class TestView extends PaymentView {
        int gatewayChanges;

        TestView() {
            super(new StripeImplementation(0));
        }

        @Override
        public String getViewTitle() {
            return "Test";
        }

        @Override
        protected void onGatewayChanged() {
            gatewayChanges++;
        }

        PaymentResult pay() {
            return processPayment(new PaymentRequest("Jane Doe", "jane@example.com",
                    new BigDecimal("25.00"), "USD", "Test"));
        }
    }

    @Test
    void viewDelegatesToCurrentGatewayAndCanSwapIt() {
        TestView view = new TestView();
        assertEquals("Stripe", view.pay().provider());

        view.setGateway(new PayPalImplementation(0));

        assertEquals("PayPal", view.pay().provider());
        assertEquals(1, view.gatewayChanges);
    }
}

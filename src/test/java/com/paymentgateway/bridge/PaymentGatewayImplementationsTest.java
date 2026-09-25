package com.paymentgateway.bridge;

import com.paymentgateway.bridge.implementation.PayPalImplementation;
import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.bridge.implementation.StripeImplementation;
import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentGatewayImplementationsTest {

    private final PaymentGatewayAPI stripe = new StripeImplementation(0);
    private final PaymentGatewayAPI payPal = new PayPalImplementation(0);

    private static PaymentRequest request(String email, String amount, String currency) {
        return new PaymentRequest("Jane Doe", email, new BigDecimal(amount), currency, "Test");
    }

    @Test
    void eachGatewayCalculatesItsOwnFee() {
        assertEquals(new BigDecimal("3.20"), stripe.calculateFee(new BigDecimal("100")));  // 2.9% + 0.30
        assertEquals(new BigDecimal("3.98"), payPal.calculateFee(new BigDecimal("100")));  // 3.49% + 0.49
    }

    @Test
    void stripeApprovesValidCharge() {
        PaymentResult result = stripe.charge(request("jane@example.com", "120.00", "USD"));

        assertTrue(result.success());
        assertTrue(result.transactionId().startsWith("ch_"));
        assertEquals(new BigDecimal("116.22"), result.netAmount());
    }

    @Test
    void stripeDeclinesTestAmountEndingIn13() {
        PaymentResult result = stripe.charge(request("jane@example.com", "50.13", "USD"));

        assertFalse(result.success());
        assertTrue(result.message().contains("card_declined"));
    }

    @Test
    void payPalRejectsBlockedAccountsAndUnsupportedCurrencies() {
        assertFalse(payPal.charge(request("blocked.user@example.com", "10.00", "USD")).success());
        assertFalse(payPal.charge(request("jane@example.com", "10.00", "MXN")).success());
        assertTrue(payPal.charge(request("jane@example.com", "10.00", "CAD")).success());
    }

    @Test
    void paymentRequestValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> request("not-an-email", "10.00", "USD"));
        assertThrows(IllegalArgumentException.class, () -> request("jane@example.com", "0", "USD"));
    }
}

package com.paymentgateway.adapter;

/**
 * Raised when an invoice provider cannot issue an invoice.
 */
public class InvoiceProcessingException extends RuntimeException {

    public InvoiceProcessingException(String message) {
        super(message);
    }

    public InvoiceProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

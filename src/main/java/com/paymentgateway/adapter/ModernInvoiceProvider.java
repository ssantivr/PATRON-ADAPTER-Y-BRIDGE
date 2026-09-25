package com.paymentgateway.adapter;

import com.paymentgateway.model.InvoiceRequest;
import com.paymentgateway.model.InvoiceResponse;

/**
 * ADAPTER PATTERN - Target.
 * <p>
 * The modern, JSON-friendly invoicing interface the rest of the application uses.
 * Views only know this interface; they never see SOAP or XML.
 */
public interface ModernInvoiceProvider {

    /** Display name of the provider behind this interface. */
    String getProviderName();

    /**
     * Issues an invoice.
     *
     * @throws InvoiceProcessingException if the provider rejects the request
     */
    InvoiceResponse issueInvoice(InvoiceRequest request);
}

package com.paymentgateway.adapter;

/**
 * Steps the {@link LegacyInvoiceAdapter} goes through while translating a call.
 * Listeners (e.g. the UI) can observe them without knowing anything about SOAP or XML.
 */
public enum AdapterStage {
    /** The modern request object arrived at the adapter. */
    REQUEST_RECEIVED,
    /** The request was translated into a SOAP envelope and sent to the legacy service. */
    SOAP_REQUEST_SENT,
    /** The legacy service answered with a SOAP/XML response. */
    SOAP_RESPONSE_RECEIVED,
    /** The response was mapped back into a modern {@code InvoiceResponse}. */
    RESPONSE_MAPPED
}

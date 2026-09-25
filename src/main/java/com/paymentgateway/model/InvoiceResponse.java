package com.paymentgateway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modern invoice representation used inside the application.
 * It can be serialized to JSON, which is the format our internal services expect.
 */
public record InvoiceResponse(String invoiceNumber,
                              InvoiceStatus status,
                              String customerName,
                              BigDecimal subtotal,
                              BigDecimal tax,
                              BigDecimal total,
                              String currency,
                              LocalDateTime issuedAt,
                              String paymentReference) {

    public String toJson() {
        return """
                {
                  "invoiceNumber": "%s",
                  "status": "%s",
                  "customerName": "%s",
                  "paymentReference": "%s",
                  "currency": "%s",
                  "subtotal": %s,
                  "tax": %s,
                  "total": %s,
                  "issuedAt": "%s"
                }""".formatted(
                escapeJson(invoiceNumber),
                status,
                escapeJson(customerName),
                escapeJson(paymentReference),
                escapeJson(currency),
                subtotal.toPlainString(),
                tax.toPlainString(),
                total.toPlainString(),
                issuedAt);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

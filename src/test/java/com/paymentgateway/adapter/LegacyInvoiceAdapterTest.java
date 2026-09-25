package com.paymentgateway.adapter;

import com.paymentgateway.legacy.LegacySoapInvoiceService;
import com.paymentgateway.model.InvoiceRequest;
import com.paymentgateway.model.InvoiceResponse;
import com.paymentgateway.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyInvoiceAdapterTest {

    private final LegacyInvoiceAdapter adapter = new LegacyInvoiceAdapter(new LegacySoapInvoiceService(0));

    private static InvoiceRequest request(String customer, String amount, String reference) {
        return new InvoiceRequest(customer, "jane@example.com", new BigDecimal(amount), "USD",
                "Premium plan", reference);
    }

    @Test
    void translatesLegacySoapResponseIntoModernInvoice() {
        InvoiceResponse invoice = adapter.issueInvoice(request("Jane Doe", "100.00", "ch_123"));

        assertTrue(invoice.invoiceNumber().startsWith("LEG-"));
        assertEquals(InvoiceStatus.ISSUED, invoice.status());          // "A" -> ISSUED
        assertEquals(new BigDecimal("19.00"), invoice.tax());
        assertEquals(new BigDecimal("119.00"), invoice.total());
        assertEquals("ch_123", invoice.paymentReference());
        assertTrue(invoice.toJson().contains("\"status\": \"ISSUED\""));
    }

    @Test
    void mapsMissingPaymentReferenceToPending() {
        InvoiceResponse invoice = adapter.issueInvoice(request("Jane Doe", "10.00", ""));
        assertEquals(InvoiceStatus.PENDING, invoice.status());       // "P" -> PENDING
    }

    @Test
    void escapesSpecialCharactersInBothDirections() {
        InvoiceResponse invoice = adapter.issueInvoice(request("Smith & Sons <Ltd>", "10.00", "ch_1"));
        assertEquals("Smith & Sons <Ltd>", invoice.customerName());
    }

    @Test
    void reportsEveryTranslationStageInOrder() {
        List<AdapterStage> stages = new ArrayList<>();
        adapter.setStageListener(stages::add);

        adapter.issueInvoice(request("Jane Doe", "10.00", "ch_1"));

        assertEquals(List.of(AdapterStage.values()), stages);
    }

    @Test
    void convertsSoapFaultIntoException() {
        assertThrows(InvoiceProcessingException.class,
                () -> adapter.issueInvoice(request("Jane Doe", "-5.00", "ch_1")));
    }
}

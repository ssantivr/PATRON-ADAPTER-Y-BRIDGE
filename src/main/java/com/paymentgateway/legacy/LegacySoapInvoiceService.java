package com.paymentgateway.legacy;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ADAPTER PATTERN - Adaptee.
 * <p>
 * Simulates an old third-party invoicing system. It is <b>incompatible</b> with our code:
 * <ul>
 *     <li>it only speaks SOAP/XML (we use Java objects and JSON),</li>
 *     <li>it uses cryptic field names such as {@code CUST_NAME} or {@code AMT_VALUE},</li>
 *     <li>it returns single-letter status codes ("A", "P", "R"),</li>
 *     <li>it formats dates as {@code dd/MM/yyyy HH:mm:ss}.</li>
 * </ul>
 * We treat this class as external code: it must NOT be modified to fit our interfaces.
 */
public class LegacySoapInvoiceService {

    public static final String ACTION_CREATE_INVOICE = "CreateInvoice";

    private static final BigDecimal TAX_RATE = new BigDecimal("0.19");
    private static final DateTimeFormatter LEGACY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AtomicInteger invoiceSequence = new AtomicInteger(1000);
    private final long simulatedLatencyMillis;

    public LegacySoapInvoiceService() {
        this(600);
    }

    /** @param simulatedLatencyMillis artificial delay of this slow old system (use 0 in tests) */
    public LegacySoapInvoiceService(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    /**
     * Single SOAP entry point, the way many legacy services were built.
     *
     * @param soapAction   operation name (only "CreateInvoice" is supported)
     * @param soapEnvelope request as a SOAP XML envelope
     * @return response as a SOAP XML envelope (may contain a soap:Fault)
     */
    public String executeSoapCall(String soapAction, String soapEnvelope) {
        simulateLatency();
        if (!ACTION_CREATE_INVOICE.equals(soapAction)) {
            return buildFault("soap:Client", "Unknown SOAPAction: " + soapAction);
        }

        try {
            Document request = parse(soapEnvelope);
            String customerName = readTag(request, "CUST_NAME");
            String currency = readTag(request, "CURR_CODE");
            String paymentReference = readTag(request, "REF_TXN");
            BigDecimal amount = new BigDecimal(readTag(request, "AMT_VALUE"));

            if (amount.signum() <= 0) {
                return buildFault("soap:Client", "AMT_VALUE must be positive");
            }

            BigDecimal subtotal = amount.setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(tax);
            // "A" = approved/issued, "P" = pending (no payment reference yet)
            String statusCode = paymentReference.isBlank() ? "P" : "A";
            String invoiceNumber = "LEG-" + invoiceSequence.incrementAndGet();

            return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body>
                        <CreateInvoiceResponse>
                          <INV_NUMBER>%s</INV_NUMBER>
                          <INV_STATUS>%s</INV_STATUS>
                          <CUST_NAME>%s</CUST_NAME>
                          <SUBTOTAL>%s</SUBTOTAL>
                          <TAX_AMT>%s</TAX_AMT>
                          <TOTAL_AMT>%s</TOTAL_AMT>
                          <CURR_CODE>%s</CURR_CODE>
                          <REF_TXN>%s</REF_TXN>
                          <ISSUE_DT>%s</ISSUE_DT>
                        </CreateInvoiceResponse>
                      </soap:Body>
                    </soap:Envelope>""".formatted(
                    invoiceNumber, statusCode, escapeXml(customerName),
                    subtotal.toPlainString(), tax.toPlainString(), total.toPlainString(),
                    escapeXml(currency), escapeXml(paymentReference),
                    LocalDateTime.now().format(LEGACY_DATE_FORMAT));
        } catch (Exception e) {
            return buildFault("soap:Server", "Malformed request: " + e.getMessage());
        }
    }

    private void simulateLatency() {
        if (simulatedLatencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static String readTag(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("missing <" + tagName + ">");
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String buildFault(String code, String message) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <soap:Fault>
                      <faultcode>%s</faultcode>
                      <faultstring>%s</faultstring>
                    </soap:Fault>
                  </soap:Body>
                </soap:Envelope>""".formatted(code, escapeXml(message));
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

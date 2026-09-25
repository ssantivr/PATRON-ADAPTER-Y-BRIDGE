package com.paymentgateway.adapter;

import com.paymentgateway.legacy.LegacySoapInvoiceService;
import com.paymentgateway.model.InvoiceRequest;
import com.paymentgateway.model.InvoiceResponse;
import com.paymentgateway.model.InvoiceStatus;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ADAPTER PATTERN - Adapter (object adapter, uses composition).
 * <p>
 * Implements our modern {@link ModernInvoiceProvider} interface and translates every call
 * to the incompatible {@link LegacySoapInvoiceService}:
 * <ol>
 *     <li>{@link InvoiceRequest} (Java object) &rarr; SOAP XML envelope with legacy field names</li>
 *     <li>calls the legacy service</li>
 *     <li>SOAP XML response &rarr; {@link InvoiceResponse} (serializable to JSON)</li>
 *     <li>legacy status codes and date format &rarr; {@link InvoiceStatus} and {@link LocalDateTime}</li>
 * </ol>
 */
public class LegacyInvoiceAdapter implements ModernInvoiceProvider {

    private static final DateTimeFormatter LEGACY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LegacySoapInvoiceService legacyService;
    private Consumer<AdapterStage> stageListener = stage -> { };

    public LegacyInvoiceAdapter(LegacySoapInvoiceService legacyService) {
        this.legacyService = Objects.requireNonNull(legacyService, "legacyService");
    }

    /**
     * Optional hook to follow the translation steps (the UI uses it to animate the flow).
     */
    public void setStageListener(Consumer<AdapterStage> listener) {
        this.stageListener = listener == null ? stage -> { } : listener;
    }

    @Override
    public String getProviderName() {
        return "Legacy SOAP Invoicing (via adapter)";
    }

    @Override
    public InvoiceResponse issueInvoice(InvoiceRequest request) {
        Objects.requireNonNull(request, "request");
        stageListener.accept(AdapterStage.REQUEST_RECEIVED);

        String soapRequest = toSoapEnvelope(request);
        stageListener.accept(AdapterStage.SOAP_REQUEST_SENT);

        String soapResponse = legacyService.executeSoapCall(LegacySoapInvoiceService.ACTION_CREATE_INVOICE, soapRequest);
        stageListener.accept(AdapterStage.SOAP_RESPONSE_RECEIVED);

        InvoiceResponse response = fromSoapResponse(soapResponse);
        stageListener.accept(AdapterStage.RESPONSE_MAPPED);
        return response;
    }

    // ---------------------------------------------------------------- translation: modern -> legacy

    private String toSoapEnvelope(InvoiceRequest request) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <CreateInvoice>
                      <CUST_NAME>%s</CUST_NAME>
                      <CUST_MAIL>%s</CUST_MAIL>
                      <AMT_VALUE>%s</AMT_VALUE>
                      <CURR_CODE>%s</CURR_CODE>
                      <DESC_TEXT>%s</DESC_TEXT>
                      <REF_TXN>%s</REF_TXN>
                    </CreateInvoice>
                  </soap:Body>
                </soap:Envelope>""".formatted(
                escapeXml(request.customerName()),
                escapeXml(request.customerEmail()),
                request.amount().toPlainString(),
                escapeXml(request.currency()),
                escapeXml(request.description()),
                escapeXml(request.paymentReference()));
    }

    // ---------------------------------------------------------------- translation: legacy -> modern

    private InvoiceResponse fromSoapResponse(String soapResponse) {
        Document document = parse(soapResponse);

        if (document.getElementsByTagName("soap:Fault").getLength() > 0) {
            throw new InvoiceProcessingException("Legacy invoicing service rejected the request: "
                    + readTag(document, "faultstring"));
        }

        return new InvoiceResponse(
                readTag(document, "INV_NUMBER"),
                mapStatus(readTag(document, "INV_STATUS")),
                readTag(document, "CUST_NAME"),
                new BigDecimal(readTag(document, "SUBTOTAL")),
                new BigDecimal(readTag(document, "TAX_AMT")),
                new BigDecimal(readTag(document, "TOTAL_AMT")),
                readTag(document, "CURR_CODE"),
                LocalDateTime.parse(readTag(document, "ISSUE_DT"), LEGACY_DATE_FORMAT),
                readTag(document, "REF_TXN"));
    }

    static InvoiceStatus mapStatus(String legacyCode) {
        return switch (legacyCode) {
            case "A" -> InvoiceStatus.ISSUED;
            case "P" -> InvoiceStatus.PENDING;
            case "R" -> InvoiceStatus.REJECTED;
            default -> throw new InvoiceProcessingException("Unknown legacy status code: " + legacyCode);
        };
    }

    // ---------------------------------------------------------------- XML helpers

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new InvoiceProcessingException("Could not parse legacy SOAP response.", e);
        }
    }

    private static String readTag(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new InvoiceProcessingException("Legacy response is missing <" + tagName + ">.");
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

package com.paymentgateway.bridge.abstraction;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.adapter.AdapterStage;
import com.paymentgateway.adapter.InvoiceProcessingException;
import com.paymentgateway.adapter.ModernInvoiceProvider;
import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.model.InvoiceRequest;
import com.paymentgateway.model.InvoiceResponse;
import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;
import com.paymentgateway.ui.components.AdapterFlowPanel;
import com.paymentgateway.ui.components.CardPanel;
import com.paymentgateway.ui.components.InvoicePreviewPanel;
import com.paymentgateway.ui.components.KeyValuePanel;
import com.paymentgateway.ui.components.PaymentFormPanel;
import com.paymentgateway.ui.components.StatusBadge;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * BRIDGE PATTERN - Refined Abstraction #2.
 * <p>
 * Richer view that charges the customer through the current gateway (Bridge) and then
 * issues an invoice through a {@link ModernInvoiceProvider} (Adapter target).
 * It never knows that the invoice actually comes from a legacy SOAP service.
 */
public class AdvancedInvoiceView extends PaymentView {

    private final ModernInvoiceProvider invoiceProvider;

    private final PaymentFormPanel form = new PaymentFormPanel();
    private final JButton issueButton = new JButton();
    private final StatusBadge paymentBadge = new StatusBadge();
    private final StatusBadge invoiceBadge = new StatusBadge();
    private final KeyValuePanel summary = new KeyValuePanel();
    private final InvoicePreviewPanel invoicePreview = new InvoicePreviewPanel();
    private final AdapterFlowPanel adapterFlow = new AdapterFlowPanel();

    /** Result of the background work: a payment and, if it succeeded, an invoice or an error. */
    private record Outcome(PaymentResult payment, InvoiceRequest invoiceRequest,
                           InvoiceResponse invoice, String invoiceError) {
    }

    public AdvancedInvoiceView(PaymentGatewayAPI gateway, ModernInvoiceProvider invoiceProvider) {
        super(gateway);
        this.invoiceProvider = Objects.requireNonNull(invoiceProvider, "invoiceProvider");
        buildLayout();
        issueButton.addActionListener(e -> onIssueClicked());
        resetStatus();
        onGatewayChanged();
    }

    @Override
    public String getViewTitle() {
        return "Advanced Invoice View";
    }

    @Override
    protected void onGatewayChanged() {
        form.setSupportedCurrencies(gateway.getSupportedCurrencies());
        issueButton.setText("Charge with " + gateway.getProviderName() + " & Issue Invoice");
    }

    /** Receives progress from the adapter. Safe to call from any thread. */
    public void onAdapterStage(AdapterStage stage) {
        SwingUtilities.invokeLater(() -> adapterFlow.advanceTo(stage));
    }

    // ---------------------------------------------------------------- layout

    private void buildLayout() {
        setLayout(new GridLayout(1, 2, 16, 0));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        issueButton.putClientProperty(FlatClientProperties.STYLE, AppTheme.PRIMARY_BUTTON_STYLE);

        JLabel providerLabel = new JLabel("Invoices issued by: " + invoiceProvider.getProviderName());
        providerLabel.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);

        JPanel actions = new JPanel(new BorderLayout(0, 10));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));
        actions.add(providerLabel, BorderLayout.NORTH);
        actions.add(issueButton, BorderLayout.SOUTH);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(form, BorderLayout.CENTER);
        formContent.add(actions, BorderLayout.SOUTH);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badges.setOpaque(false);
        badges.add(paymentBadge);
        badges.add(Box.createHorizontalStrut(8));
        badges.add(invoiceBadge);

        JPanel statusContent = new JPanel(new BorderLayout(0, 14));
        statusContent.setOpaque(false);
        statusContent.add(badges, BorderLayout.NORTH);
        statusContent.add(summary, BorderLayout.CENTER);

        JPanel leftColumn = new JPanel(new BorderLayout(0, 16));
        leftColumn.setOpaque(false);
        leftColumn.add(new CardPanel("Invoice Details", formContent), BorderLayout.NORTH);
        leftColumn.add(new CardPanel("Payment Status", statusContent), BorderLayout.CENTER);

        JPanel rightColumn = new JPanel(new BorderLayout(0, 16));
        rightColumn.setOpaque(false);
        rightColumn.add(new CardPanel(null, invoicePreview), BorderLayout.CENTER);
        rightColumn.add(new CardPanel("How the Adapter Works", adapterFlow), BorderLayout.SOUTH);

        add(leftColumn);
        add(rightColumn);
    }

    // ---------------------------------------------------------------- behavior

    private void resetStatus() {
        paymentBadge.setStatus(StatusBadge.Status.IDLE, "Payment: idle");
        invoiceBadge.setStatus(StatusBadge.Status.IDLE, "Invoice: idle");
        summary.setEntries(Map.of("Info", "Fill in the form and issue an invoice."));
    }

    private void onIssueClicked() {
        PaymentRequest request;
        try {
            request = form.buildRequest();
        } catch (IllegalArgumentException ex) {
            paymentBadge.setStatus(StatusBadge.Status.FAILED, "Invalid input");
            invoiceBadge.setStatus(StatusBadge.Status.IDLE, "Invoice: not requested");
            summary.setEntries(Map.of("Error", ex.getMessage()));
            return;
        }

        setBusy(true);
        adapterFlow.reset();
        invoicePreview.showEmpty("Processing payment...",
                "The invoice will be generated as soon as the payment is approved.");

        new SwingWorker<Outcome, Void>() {
            @Override
            protected Outcome doInBackground() {
                PaymentResult payment = processPayment(request);                  // Bridge
                if (!payment.success()) {
                    return new Outcome(payment, null, null, null);
                }
                InvoiceRequest invoiceRequest = InvoiceRequest.fromPayment(request, payment);
                try {
                    InvoiceResponse invoice = invoiceProvider.issueInvoice(invoiceRequest); // Adapter
                    return new Outcome(payment, invoiceRequest, invoice, null);
                } catch (InvoiceProcessingException ex) {
                    return new Outcome(payment, invoiceRequest, null, ex.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    showOutcome(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    setBusy(false);
                } catch (ExecutionException ex) {
                    paymentBadge.setStatus(StatusBadge.Status.FAILED, "Unexpected error");
                    summary.setEntries(Map.of("Error", String.valueOf(ex.getCause().getMessage())));
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        issueButton.setEnabled(!busy);
        form.setInputsEnabled(!busy);
        if (busy) {
            paymentBadge.setStatus(StatusBadge.Status.PROCESSING, "Payment: " + gateway.getProviderName() + "...");
            invoiceBadge.setStatus(StatusBadge.Status.IDLE, "Invoice: waiting");
        }
    }

    private void showOutcome(Outcome outcome) {
        PaymentResult payment = outcome.payment();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Gateway", payment.provider());
        details.put("Transaction ID", payment.transactionId());
        details.put("Charged", formatMoney(payment.amount(), payment.currency()));
        details.put("Gateway fee", formatMoney(payment.fee(), payment.currency()));
        details.put("Net received", formatMoney(payment.netAmount(), payment.currency()));

        if (!payment.success()) {
            paymentBadge.setStatus(StatusBadge.Status.FAILED, "Payment: declined");
            invoiceBadge.setStatus(StatusBadge.Status.IDLE, "Invoice: skipped");
            details.put("Reason", payment.message());
            summary.setEntries(details);
            invoicePreview.showEmpty("No invoice issued", "The payment was declined, so no invoice was requested.");
            setBusy(false);
            return;
        }

        paymentBadge.setStatus(StatusBadge.Status.SUCCESS, "Payment: approved");
        summary.setEntries(details);

        if (outcome.invoiceError() != null) {
            adapterFlow.fail();
            invoiceBadge.setStatus(StatusBadge.Status.FAILED, "Invoice: failed");
            invoicePreview.showEmpty("Invoice could not be issued", outcome.invoiceError());
            setBusy(false);
            return;
        }

        invoiceBadge.setStatus(StatusBadge.Status.PROCESSING, "Invoice: generating...");
        adapterFlow.whenComplete(() -> {
            invoiceBadge.setStatus(StatusBadge.Status.SUCCESS, "Invoice: " + outcome.invoice().status());
            invoicePreview.showInvoice(outcome.invoiceRequest(), outcome.invoice());
            setBusy(false);
        });
    }
}

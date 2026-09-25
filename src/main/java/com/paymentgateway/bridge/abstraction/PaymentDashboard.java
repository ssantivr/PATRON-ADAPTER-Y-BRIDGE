package com.paymentgateway.bridge.abstraction;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.model.PaymentRequest;
import com.paymentgateway.model.PaymentResult;
import com.paymentgateway.ui.components.CardPanel;
import com.paymentgateway.ui.components.KeyValuePanel;
import com.paymentgateway.ui.components.PaymentFormPanel;
import com.paymentgateway.ui.components.StatTile;
import com.paymentgateway.ui.components.StatusBadge;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * BRIDGE PATTERN - Refined Abstraction #1.
 * <p>
 * Simple dashboard: take a payment, see the result, and keep a session history.
 * Works with ANY {@link PaymentGatewayAPI} without knowing which one.
 */
public class PaymentDashboard extends PaymentView {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int STATUS_COLUMN = 5;

    private final PaymentFormPanel form = new PaymentFormPanel();
    private final JLabel feePreviewLabel = new JLabel();
    private final JButton payButton = new JButton();
    private final StatusBadge statusBadge = new StatusBadge();
    private final KeyValuePanel resultDetails = new KeyValuePanel();
    private final StatTile totalTile = new StatTile("Transactions", "processed this session", AppTheme.ACCENT);
    private final StatTile approvedTile = new StatTile("Approved", "payments captured", AppTheme.SUCCESS);
    private final StatTile declinedTile = new StatTile("Declined", "rejected by the gateway", AppTheme.ERROR);
    private final StatTile rateTile = new StatTile("Approval Rate", "approved / total", AppTheme.VIOLET);
    private int approvedCount;
    private int declinedCount;
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"Time", "Gateway", "Customer", "Amount", "Fee", "Status", "Transaction ID"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public PaymentDashboard(PaymentGatewayAPI gateway) {
        super(gateway);
        buildLayout();
        form.onAmountOrCurrencyChanged(this::refreshFeePreview);
        payButton.addActionListener(e -> onPayClicked());
        resultDetails.setEntries(Map.of("Info", "Submit a payment to see the result here."));
        onGatewayChanged();
    }

    @Override
    public String getViewTitle() {
        return "Payment Dashboard";
    }

    @Override
    protected void onGatewayChanged() {
        form.setSupportedCurrencies(gateway.getSupportedCurrencies());
        payButton.setText("Pay with " + gateway.getProviderName());
        refreshFeePreview();
    }

    // ---------------------------------------------------------------- layout

    private void buildLayout() {
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        payButton.putClientProperty(FlatClientProperties.STYLE, AppTheme.PRIMARY_BUTTON_STYLE);
        feePreviewLabel.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);

        JPanel actions = new JPanel(new BorderLayout(0, 10));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));
        actions.add(feePreviewLabel, BorderLayout.NORTH);
        actions.add(payButton, BorderLayout.SOUTH);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(form, BorderLayout.CENTER);
        formContent.add(actions, BorderLayout.SOUTH);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(statusBadge);

        JPanel resultContent = new JPanel(new BorderLayout(0, 14));
        resultContent.setOpaque(false);
        resultContent.add(badgeRow, BorderLayout.NORTH);
        resultContent.add(resultDetails, BorderLayout.CENTER);

        JPanel topRow = new JPanel(new GridLayout(1, 2, 16, 0));
        topRow.setOpaque(false);
        topRow.add(new CardPanel("New Payment", formContent));
        topRow.add(new CardPanel("Last Transaction", resultContent));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 0));
        tiles.setOpaque(false);
        tiles.add(totalTile);
        tiles.add(approvedTile);
        tiles.add(declinedTile);
        tiles.add(rateTile);
        rateTile.setValue("-");

        JPanel body = new JPanel(new BorderLayout(16, 16));
        body.setOpaque(false);
        body.add(topRow, BorderLayout.NORTH);
        body.add(new CardPanel("Session History", buildHistoryTable()), BorderLayout.CENTER);

        add(tiles, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private JScrollPane buildHistoryTable() {
        JTable table = new JTable(historyModel);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(6).setPreferredWidth(220);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    // ---------------------------------------------------------------- behavior

    private void refreshFeePreview() {
        String currency = form.getSelectedCurrency();
        String text = form.currentAmount()
                .filter(amount -> amount.signum() > 0)
                .map(amount -> {
                    BigDecimal fee = gateway.calculateFee(amount);
                    return "%s fee: %s   •   You receive: %s".formatted(gateway.getProviderName(),
                            formatMoney(fee, currency), formatMoney(amount.subtract(fee), currency));
                })
                .orElse("Enter a valid amount to preview gateway fees.");
        feePreviewLabel.setText(text);
    }

    private void onPayClicked() {
        PaymentRequest request;
        try {
            request = form.buildRequest();
        } catch (IllegalArgumentException ex) {
            statusBadge.setStatus(StatusBadge.Status.FAILED, "Invalid input");
            resultDetails.setEntries(Map.of("Error", ex.getMessage()));
            return;
        }

        setBusy(true);
        new SwingWorker<PaymentResult, Void>() {
            @Override
            protected PaymentResult doInBackground() {
                return processPayment(request);
            }

            @Override
            protected void done() {
                try {
                    showResult(request, get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusBadge.setStatus(StatusBadge.Status.FAILED, "Unexpected error");
                    resultDetails.setEntries(Map.of("Error", String.valueOf(ex.getCause().getMessage())));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        payButton.setEnabled(!busy);
        form.setInputsEnabled(!busy);
        if (busy) {
            statusBadge.setStatus(StatusBadge.Status.PROCESSING,
                    "Contacting " + gateway.getProviderName() + "...");
        }
    }

    private void showResult(PaymentRequest request, PaymentResult result) {
        statusBadge.setStatus(result.success() ? StatusBadge.Status.SUCCESS : StatusBadge.Status.FAILED);

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Gateway", result.provider());
        details.put("Transaction ID", result.transactionId());
        details.put("Amount", formatMoney(result.amount(), result.currency()));
        details.put("Fee", formatMoney(result.fee(), result.currency()));
        details.put("Net amount", formatMoney(result.netAmount(), result.currency()));
        details.put("Message", result.message());
        details.put("Processed at", result.processedAt().format(TIME_FORMAT));
        resultDetails.setEntries(details);

        historyModel.insertRow(0, new Object[]{
                result.processedAt().format(TIME_FORMAT),
                result.provider(),
                request.customerName(),
                formatMoney(result.amount(), result.currency()),
                formatMoney(result.fee(), result.currency()),
                result.success() ? "Approved" : "Declined",
                result.transactionId()
        });
        updateStats(result.success());
    }

    private void updateStats(boolean approved) {
        if (approved) {
            approvedCount++;
        } else {
            declinedCount++;
        }
        int total = approvedCount + declinedCount;
        totalTile.setValue(String.valueOf(total));
        approvedTile.setValue(String.valueOf(approvedCount));
        declinedTile.setValue(String.valueOf(declinedCount));
        rateTile.setValue(Math.round(approvedCount * 100.0 / total) + "%");
    }

    /** Colors the Status column green/red. */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                cell.setForeground("Approved".equals(value) ? AppTheme.SUCCESS : AppTheme.ERROR);
            }
            return cell;
        }
    }
}

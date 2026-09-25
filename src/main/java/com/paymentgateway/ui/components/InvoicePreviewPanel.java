package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.model.InvoiceRequest;
import com.paymentgateway.model.InvoiceResponse;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders an {@link InvoiceResponse} as a clean, document-like invoice.
 * Shows a friendly empty state while there is no invoice.
 */
public class InvoicePreviewPanel extends JPanel {

    private static final String EMPTY_CARD = "empty";
    private static final String INVOICE_CARD = "invoice";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm", Locale.US);
    private static final String WHITE_TEXT = "foreground: #FFFFFF";
    private static final String BAND_CAPTION = "foreground: #E0E7FF; font: bold -2";

    private final CardLayout cardLayout = new CardLayout();
    private final JLabel emptyTitle = new JLabel("", SwingConstants.CENTER);
    private final JLabel emptyHint = new JLabel("", SwingConstants.CENTER);

    private final JLabel invoiceNumber = new JLabel();
    private final JLabel bandTotal = new JLabel();
    private final JLabel billedName = new JLabel();
    private final JLabel billedEmail = new JLabel();
    private final JLabel issuedAt = new JLabel();
    private final JLabel paymentReference = new JLabel();
    private final JLabel itemDescription = new JLabel();
    private final JLabel itemAmount = new JLabel();
    private final JLabel subtotal = new JLabel();
    private final JLabel tax = new JLabel();
    private final JLabel total = new JLabel();
    private final StatusBadge statusBadge = new StatusBadge();

    public InvoicePreviewPanel() {
        setLayout(cardLayout);
        setOpaque(false);
        add(buildEmptyState(), EMPTY_CARD);
        add(buildInvoice(), INVOICE_CARD);
        showEmpty("No invoice yet", "Charge a customer and the invoice will appear here.");
    }

    public void showEmpty(String title, String hint) {
        emptyTitle.setText(title);
        emptyHint.setText(hint);
        cardLayout.show(this, EMPTY_CARD);
    }

    public void showInvoice(InvoiceRequest request, InvoiceResponse invoice) {
        String currency = invoice.currency();
        invoiceNumber.setText("# " + invoice.invoiceNumber());
        bandTotal.setText(money(invoice.total(), currency));
        billedName.setText(invoice.customerName());
        billedEmail.setText(request.customerEmail());
        issuedAt.setText(invoice.issuedAt().format(DATE_FORMAT));
        paymentReference.setText(invoice.paymentReference());
        paymentReference.setToolTipText(invoice.paymentReference());
        itemDescription.setText(request.description().isBlank() ? "Payment" : request.description());
        itemAmount.setText(money(invoice.subtotal(), currency));
        subtotal.setText(money(invoice.subtotal(), currency));
        tax.setText(money(invoice.tax(), currency));
        total.setText(money(invoice.total(), currency));

        switch (invoice.status()) {
            case ISSUED -> statusBadge.setStatus(StatusBadge.Status.SUCCESS, "Paid & issued");
            case PENDING -> statusBadge.setStatus(StatusBadge.Status.PROCESSING, "Pending payment");
            case REJECTED -> statusBadge.setStatus(StatusBadge.Status.FAILED, "Rejected");
        }
        cardLayout.show(this, INVOICE_CARD);
    }

    // ---------------------------------------------------------------- empty state

    private JPanel buildEmptyState() {
        emptyTitle.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        emptyHint.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(0, 0, 14, 0);
        panel.add(new DocumentIcon(), c);
        c.insets = new Insets(0, 0, 4, 0);
        panel.add(emptyTitle, c);
        panel.add(emptyHint, c);
        return panel;
    }

    // ---------------------------------------------------------------- invoice document

    private JPanel buildInvoice() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.insets = new Insets(0, 0, 14, 0);
        panel.add(buildBand(), c);

        panel.add(buildMetaRow(), c);

        c.insets = new Insets(0, 0, 8, 0);
        panel.add(twoColumns(caption("DESCRIPTION"), caption("AMOUNT")), c);
        panel.add(new JSeparator(), c);
        itemDescription.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");
        c.insets = new Insets(0, 0, 10, 0);
        panel.add(twoColumns(itemDescription, itemAmount), c);
        panel.add(new JSeparator(), c);

        panel.add(buildTotals(), c);

        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(buildFooter(), c);
        return panel;
    }

    private JPanel buildBand() {
        JLabel title = new JLabel("INVOICE");
        title.putClientProperty(FlatClientProperties.STYLE, BAND_CAPTION);
        invoiceNumber.putClientProperty(FlatClientProperties.STYLE, WHITE_TEXT);
        invoiceNumber.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");

        JLabel totalCaption = new JLabel("AMOUNT DUE", SwingConstants.RIGHT);
        totalCaption.putClientProperty(FlatClientProperties.STYLE, BAND_CAPTION);
        bandTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        bandTotal.putClientProperty(FlatClientProperties.STYLE, WHITE_TEXT);
        bandTotal.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");

        JPanel left = stack(title, invoiceNumber);
        JPanel right = stack(totalCaption, bandTotal);

        GradientPanel band = new GradientPanel(new BorderLayout(), AppTheme.GRADIENT_START, AppTheme.GRADIENT_END, 16);
        band.setBorder(new EmptyBorder(14, 18, 14, 18));
        band.add(left, BorderLayout.WEST);
        band.add(right, BorderLayout.EAST);
        return band;
    }

    private JPanel buildMetaRow() {
        billedName.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");
        billedEmail.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);
        issuedAt.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");
        paymentReference.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");

        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(stack(caption("BILLED TO"), billedName, billedEmail));
        // Blank third line keeps all three columns top-aligned with "BILLED TO".
        row.add(stack(caption("ISSUED"), issuedAt, new JLabel(" ")));
        row.add(stack(caption("PAYMENT REFERENCE"), paymentReference, new JLabel(" ")));
        return row;
    }

    private JPanel buildTotals() {
        total.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        JLabel totalCaption = new JLabel("Total");
        totalCaption.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        addTotalRow(grid, 0, muted("Subtotal"), subtotal);
        addTotalRow(grid, 1, muted("Tax (VAT 19%)"), tax);
        addTotalRow(grid, 2, totalCaption, total);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(grid, BorderLayout.EAST);
        return wrapper;
    }

    private static void addTotalRow(JPanel grid, int row, JLabel label, JLabel value) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 28);
        grid.add(label, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(2, 0, 2, 0);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        grid.add(value, c);
    }

    private JPanel buildFooter() {
        JLabel issuer = new JLabel("Issued by the legacy SOAP service through the adapter");
        issuer.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);
        issuer.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");

        JPanel badgeHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeHolder.setOpaque(false);
        badgeHolder.add(statusBadge);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(badgeHolder, BorderLayout.WEST);
        footer.add(issuer, BorderLayout.EAST);
        return footer;
    }

    // ---------------------------------------------------------------- helpers

    private static JPanel stack(JComponent... components) {
        JPanel panel = new JPanel(new GridLayout(components.length, 1, 0, 2));
        panel.setOpaque(false);
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private static JPanel twoColumns(JComponent left, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, AppTheme.CAPTION_STYLE);
        return label;
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);
        return label;
    }

    private static String money(BigDecimal amount, String currency) {
        return String.format(Locale.US, "%,.2f %s", amount, currency);
    }

    /** Small illustration of a document, used in the empty state. */
    private static class DocumentIcon extends JComponent {

        DocumentIcon() {
            setPreferredSize(new Dimension(72, 86));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color accent = AppTheme.ACCENT;

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillOval(0, 14, 72, 72);

                int x = 16;
                int y = 4;
                int w = 40;
                int h = 52;
                g2.setColor(CardPanel.cardColor());
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(accent);
                g2.drawRoundRect(x, y, w, h, 10, 10);

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
                g2.fillRoundRect(x + 8, y + 12, 24, 4, 4, 4);
                g2.fillRoundRect(x + 8, y + 22, 18, 4, 4, 4);
                g2.fillRoundRect(x + 8, y + 32, 22, 4, 4, 4);

                g2.setColor(AppTheme.SUCCESS);
                g2.fillOval(x + w - 12, y + h - 12, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = x + w - 12;
                int cy = y + h - 12;
                g2.drawPolyline(new int[]{cx + 6, cx + 10, cx + 16}, new int[]{cy + 11, cy + 15, cy + 7}, 3);
            } finally {
                g2.dispose();
            }
        }
    }
}

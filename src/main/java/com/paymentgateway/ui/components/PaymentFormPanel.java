package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.model.PaymentRequest;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Reusable customer + payment form shared by all payment views.
 * It knows nothing about gateways: it only collects input and builds a {@link PaymentRequest}.
 */
public class PaymentFormPanel extends JPanel {

    private final JTextField customerField = new JTextField("Jane Doe", 20);
    private final JTextField emailField = new JTextField("jane.doe@example.com");
    private final JTextField amountField = new JTextField("120.00");
    private final JComboBox<String> currencyBox = new JComboBox<>();
    private final JTextField descriptionField = new JTextField("Premium subscription - 12 months");

    public PaymentFormPanel() {
        super(new GridBagLayout());
        setOpaque(false);

        customerField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Full name");
        emailField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "name@company.com");
        amountField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        descriptionField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "What is this payment for?");

        addRow(0, "Customer name", customerField);
        addRow(1, "Email", emailField);
        addRow(2, "Amount", amountField);
        addRow(3, "Currency", currencyBox);
        addRow(4, "Description", descriptionField);
    }

    private void addRow(int row, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 14);
        add(new JLabel(labelText), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        add(field, c);
    }

    /**
     * @throws IllegalArgumentException with a user-friendly message if the input is invalid
     */
    public PaymentRequest buildRequest() {
        BigDecimal amount = currentAmount()
                .orElseThrow(() -> new IllegalArgumentException("Amount must be a valid number (e.g. 120.50)."));
        return new PaymentRequest(customerField.getText(), emailField.getText(), amount,
                getSelectedCurrency(), descriptionField.getText());
    }

    /** Parsed amount, or empty if the field does not contain a valid number. */
    public Optional<BigDecimal> currentAmount() {
        try {
            return Optional.of(new BigDecimal(amountField.getText().trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String getSelectedCurrency() {
        return (String) currencyBox.getSelectedItem();
    }

    /** Replaces the currency options, keeping the current selection when still available. */
    public void setSupportedCurrencies(List<String> currencies) {
        String previous = getSelectedCurrency();
        currencyBox.setModel(new DefaultComboBoxModel<>(currencies.toArray(String[]::new)));
        if (previous != null && currencies.contains(previous)) {
            currencyBox.setSelectedItem(previous);
        }
    }

    /** Runs the listener whenever the amount or the currency changes. */
    public void onAmountOrCurrencyChanged(Runnable listener) {
        amountField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        });
        currencyBox.addActionListener(e -> listener.run());
    }

    public void setInputsEnabled(boolean enabled) {
        customerField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        amountField.setEnabled(enabled);
        currencyBox.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }
}

package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

/**
 * Two-column list of "label: value" rows, used to display transaction details.
 */
public class KeyValuePanel extends JPanel {

    public KeyValuePanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public void setEntries(Map<String, String> entries) {
        removeAll();
        int row = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            JLabel key = new JLabel(entry.getKey());
            key.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);

            JLabel value = new JLabel(entry.getValue());
            value.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(3, 0, 3, 16);
            c.gridx = 0;
            add(key, c);

            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(3, 0, 3, 0);
            add(value, c);
        }
        // Push rows to the top.
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        add(new JLabel(), filler);

        revalidate();
        repaint();
    }
}

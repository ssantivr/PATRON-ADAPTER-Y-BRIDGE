package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Rounded "card" container with an optional title. Colors follow the active light/dark theme.
 */
public class CardPanel extends JPanel {

    private static final int ARC = 18;

    public CardPanel(String title, JComponent content) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 18, 18, 18));

        if (title != null) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4");
            add(titleLabel, BorderLayout.NORTH);
        }
        add(content, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintCardBackground(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /** Card surface color for the current light/dark theme. */
    public static Color cardColor() {
        Color base = UIManager.getColor("Panel.background");
        return FlatLaf.isLafDark() ? ColorFunctions.lighten(base, 0.05f) : Color.WHITE;
    }

    /** Paints the rounded card surface + border; shared by other card-like widgets. */
    public static void paintCardBackground(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(cardColor());
        g2.fillRoundRect(0, 0, width - 1, height - 1, ARC, ARC);
        g2.setColor(UIManager.getColor("Component.borderColor"));
        g2.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC);
    }
}

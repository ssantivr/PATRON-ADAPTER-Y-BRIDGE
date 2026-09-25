package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * KPI tile: small caption, big number and a hint, with a colored accent stripe.
 */
public class StatTile extends JPanel {

    private final Color accent;
    private final JLabel valueLabel = new JLabel("0");

    public StatTile(String title, String hint, Color accent) {
        super(new BorderLayout(0, 2));
        this.accent = accent;
        setOpaque(false);
        setBorder(new EmptyBorder(12, 22, 12, 16));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.putClientProperty(FlatClientProperties.STYLE, AppTheme.CAPTION_STYLE);

        valueLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");

        JLabel hintLabel = new JLabel(hint);
        hintLabel.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);
        hintLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(hintLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            CardPanel.paintCardBackground(g2, w, h);
            g2.clip(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 18, 18));

            g2.setColor(accent);
            g2.fillRect(0, 0, 5, h);

            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
            g2.fillOval(w - 56, -22, 80, 80);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
            g2.fillOval(w - 34, 14, 14, 14);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

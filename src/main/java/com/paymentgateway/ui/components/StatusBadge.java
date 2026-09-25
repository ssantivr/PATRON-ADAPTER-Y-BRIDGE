package com.paymentgateway.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Pill-shaped status indicator (Idle / Processing / Approved / Declined).
 */
public class StatusBadge extends JLabel {

    public enum Status {
        IDLE("Waiting for input", AppTheme.NEUTRAL),
        PROCESSING("Processing...", AppTheme.WARNING),
        SUCCESS("Approved", AppTheme.SUCCESS),
        FAILED("Declined", AppTheme.ERROR);

        private final String defaultText;
        private final Color color;

        Status(String defaultText, Color color) {
            this.defaultText = defaultText;
            this.color = color;
        }
    }

    private Status status = Status.IDLE;

    public StatusBadge() {
        setOpaque(false);
        setBorder(new EmptyBorder(5, 14, 5, 14));
        putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");
        setStatus(Status.IDLE);
    }

    public void setStatus(Status status) {
        setStatus(status, status.defaultText);
    }

    public void setStatus(Status status, String text) {
        this.status = status;
        setForeground(status.color);
        setText("●  " + text);
        repaint();
    }

    public Status getStatus() {
        return status;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = status.color;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 38));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

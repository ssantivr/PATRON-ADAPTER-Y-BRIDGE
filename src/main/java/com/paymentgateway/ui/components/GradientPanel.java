package com.paymentgateway.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Panel with a diagonal gradient and soft decorative circles (used for headers and banners).
 */
public class GradientPanel extends JPanel {

    private static final Color DECORATION = new Color(255, 255, 255, 22);

    private final Color startColor;
    private final Color endColor;
    private final int arc;

    public GradientPanel(LayoutManager layout, Color startColor, Color endColor, int arc) {
        super(layout);
        this.startColor = startColor;
        this.endColor = endColor;
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.clip(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            g2.setPaint(new GradientPaint(0, 0, startColor, w, h, endColor));
            g2.fillRect(0, 0, w, h);

            g2.setColor(DECORATION);
            int big = Math.max(h * 2, 140);
            g2.fillOval(w - big / 2 - 40, -big / 2, big, big);
            g2.fillOval(w - big - 60, h - big / 3, big / 2, big / 2);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

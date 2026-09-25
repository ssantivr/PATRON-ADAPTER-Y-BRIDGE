package com.paymentgateway.ui.components;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.paymentgateway.adapter.AdapterStage;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Animated diagram of the Adapter pattern at work:
 * <pre>  Invoice View  &lt;-&gt;  Invoice Adapter  &lt;-&gt;  Legacy Service</pre>
 * Nodes light up and a dot travels along the connections as the adapter reports each
 * {@link AdapterStage}. Steps are played with a minimum delay so the flow is easy to follow.
 */
public class AdapterFlowPanel extends JPanel {

    private static final String[][] NODES = {
            {"{ }", "Invoice View", "Modern objects"},
            {"↔", "Invoice Adapter", "Translator"},
            {"</>", "Legacy Service", "SOAP / XML"}
    };
    private static final String[] STEPS = {
            "Request received from the view",
            "Translated into a SOAP envelope",
            "Legacy service answered in XML",
            "Mapped back to a modern invoice"
    };
    private static final int FINAL_STEP = STEPS.length;
    private static final int STEP_DELAY_MS = 550;
    private static final int NODE_HEIGHT = 92;
    private static final int CONNECTOR_GAP = 46;

    private enum NodeState { IDLE, ACTIVE, DONE, ERROR }

    private final Timer timer = new Timer(30, e -> tick());

    /** Number of completed steps currently displayed (0..4). */
    private int progress;
    /** Number of steps reported by the adapter so far. */
    private int target;
    private boolean failPending;
    private boolean failed;
    private float phase;
    private long lastStepAt;
    private Runnable onComplete;

    public AdapterFlowPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(460, NODE_HEIGHT + 78));
    }

    public void reset() {
        timer.stop();
        progress = 0;
        target = 0;
        failPending = false;
        failed = false;
        onComplete = null;
        phase = 0;
        repaint();
    }

    public void advanceTo(AdapterStage stage) {
        target = Math.max(target, stage.ordinal() + 1);
        ensureRunning();
    }

    /** Marks the flow as failed once the already reported steps have been displayed. */
    public void fail() {
        failPending = true;
        ensureRunning();
    }

    /** Runs the action when the whole animation has finished (immediately if it already has). */
    public void whenComplete(Runnable action) {
        if (progress == FINAL_STEP && !timer.isRunning()) {
            action.run();
        } else {
            onComplete = action;
        }
    }

    private void ensureRunning() {
        if (!timer.isRunning()) {
            lastStepAt = System.currentTimeMillis();
            timer.start();
        }
    }

    private void tick() {
        phase = (phase + 0.035f) % 1f;
        long now = System.currentTimeMillis();
        if (progress < target && now - lastStepAt >= STEP_DELAY_MS) {
            progress++;
            lastStepAt = now;
        }
        if (progress == target) {
            if (failPending) {
                failed = true;
                timer.stop();
            } else if (progress == FINAL_STEP) {
                timer.stop();
                Runnable action = onComplete;
                onComplete = null;
                if (action != null) {
                    action.run();
                }
            }
        }
        repaint();
    }

    // ---------------------------------------------------------------- state -> visuals

    private NodeState nodeState(int node) {
        if (failed && node == Math.min(progress, 2)) {
            return NodeState.ERROR;
        }
        boolean running = timer.isRunning();
        return switch (node) {
            case 0 -> progress >= 1 ? NodeState.DONE : running ? NodeState.ACTIVE : NodeState.IDLE;
            case 1 -> (progress == 2 || progress == 4) ? NodeState.DONE
                    : (progress == 1 || progress == 3) ? NodeState.ACTIVE : NodeState.IDLE;
            default -> progress >= 3 ? NodeState.DONE : progress == 2 ? NodeState.ACTIVE : NodeState.IDLE;
        };
    }

    private boolean connectionReached(int connection) {
        return connection == 0 ? progress >= 1 : progress >= 2;
    }

    /** +1 = dot moves right, -1 = dot moves left, 0 = no dot. */
    private int dotDirection(int connection) {
        if (!timer.isRunning() || failed) {
            return 0;
        }
        if (connection == 0) {
            return progress == 0 ? 1 : progress == 3 ? -1 : 0;
        }
        return progress == 1 ? 1 : progress == 2 ? -1 : 0;
    }

    private static Color stateColor(NodeState state) {
        return switch (state) {
            case ACTIVE -> AppTheme.ACCENT;
            case DONE -> AppTheme.SUCCESS;
            case ERROR -> AppTheme.ERROR;
            case IDLE -> AppTheme.NEUTRAL;
        };
    }

    // ---------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);
            int nodeWidth = (getWidth() - 2 * CONNECTOR_GAP) / 3;
            for (int c = 0; c < 2; c++) {
                paintConnection(g2, c, nodeWidth);
            }
            for (int n = 0; n < NODES.length; n++) {
                paintNode(g2, n, n * (nodeWidth + CONNECTOR_GAP), nodeWidth);
            }
            paintSteps(g2, NODE_HEIGHT + 20);
        } finally {
            g2.dispose();
        }
    }

    private void paintNode(Graphics2D g2, int index, int x, int width) {
        NodeState state = nodeState(index);
        Color color = stateColor(state);
        int y = 3;
        int h = NODE_HEIGHT - 6;

        if (state == NodeState.ACTIVE) {
            int glow = Math.round((1f - phase) * 70);
            g2.setColor(withAlpha(color, glow));
            g2.setStroke(new BasicStroke(3f));
            int grow = Math.round(phase * 3);
            g2.drawRoundRect(x - grow, y - grow, width + 2 * grow - 1, h + 2 * grow - 1, 18, 18);
        }

        g2.setColor(withAlpha(color, state == NodeState.IDLE ? 12 : 30));
        g2.fillRoundRect(x, y, width - 1, h - 1, 16, 16);
        g2.setStroke(new BasicStroke(state == NodeState.IDLE ? 1f : 2f));
        g2.setColor(state == NodeState.IDLE ? UIManager.getColor("Component.borderColor") : color);
        g2.drawRoundRect(x, y, width - 1, h - 1, 16, 16);

        int bubble = 30;
        int bx = x + (width - bubble) / 2;
        int by = y + 10;
        g2.setColor(color);
        g2.fillOval(bx, by, bubble, bubble);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        drawCentered(g2, NODES[index][0], x + width / 2, by + bubble / 2);

        Font base = getFont();
        g2.setFont(base.deriveFont(Font.BOLD));
        g2.setColor(UIManager.getColor("Label.foreground"));
        drawCentered(g2, NODES[index][1], x + width / 2, by + bubble + 16);

        g2.setFont(base.deriveFont(base.getSize2D() - 1f));
        g2.setColor(UIManager.getColor("Label.disabledForeground"));
        drawCentered(g2, NODES[index][2], x + width / 2, by + bubble + 33);
    }

    private void paintConnection(Graphics2D g2, int connection, int nodeWidth) {
        int x1 = (connection + 1) * nodeWidth + connection * CONNECTOR_GAP + 7;
        int x2 = x1 + CONNECTOR_GAP - 14;
        int y = NODE_HEIGHT / 2;
        boolean reached = connectionReached(connection);

        g2.setColor(reached ? AppTheme.SUCCESS : UIManager.getColor("Component.borderColor"));
        g2.setStroke(reached
                ? new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                : new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 5f}, 0f));
        g2.drawLine(x1, y, x2, y);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(new int[]{x2 - 5, x2, x2 - 5}, new int[]{y - 4, y, y + 4}, 3);
        g2.drawPolyline(new int[]{x1 + 5, x1, x1 + 5}, new int[]{y - 4, y, y + 4}, 3);

        int direction = dotDirection(connection);
        if (direction != 0) {
            float t = direction > 0 ? phase : 1f - phase;
            int dx = Math.round(x1 + (x2 - x1) * t);
            g2.setColor(withAlpha(AppTheme.ACCENT, 70));
            g2.fillOval(dx - 8, y - 8, 16, 16);
            g2.setColor(AppTheme.ACCENT);
            g2.fillOval(dx - 4, y - 4, 8, 8);
        }
    }

    private void paintSteps(Graphics2D g2, int top) {
        int columnWidth = getWidth() / 2;
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < STEPS.length; i++) {
            int x = (i % 2) * columnWidth;
            int y = top + (i / 2) * 28;
            paintStepMarker(g2, i, x, y);

            boolean done = progress > i;
            g2.setColor(UIManager.getColor(done ? "Label.foreground" : "Label.disabledForeground"));
            int baseline = y + (18 + fm.getAscent() - fm.getDescent()) / 2;
            FlatUIUtils.drawString(this, g2, STEPS[i], x + 26, baseline);
        }
    }

    private void paintStepMarker(Graphics2D g2, int step, int x, int y) {
        int size = 18;
        if (progress > step) {
            g2.setColor(AppTheme.SUCCESS);
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{x + 5, x + 8, x + 13}, new int[]{y + 9, y + 12, y + 6}, 3);
        } else if (failed && progress == step) {
            g2.setColor(AppTheme.ERROR);
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 6, y + 6, x + 12, y + 12);
            g2.drawLine(x + 12, y + 6, x + 6, y + 12);
        } else if (timer.isRunning() && progress == step) {
            g2.setColor(AppTheme.ACCENT);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 1, y + 1, size - 2, size - 2);
            g2.setColor(withAlpha(AppTheme.ACCENT, 90 + Math.round(phase * 160)));
            g2.fillOval(x + 5, y + 5, size - 10, size - 10);
        } else {
            g2.setColor(UIManager.getColor("Component.borderColor"));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 1, y + 1, size - 2, size - 2);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int centerY) {
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        int y = centerY + (fm.getAscent() - fm.getDescent()) / 2;
        FlatUIUtils.drawString(this, g2, text, x, y);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}

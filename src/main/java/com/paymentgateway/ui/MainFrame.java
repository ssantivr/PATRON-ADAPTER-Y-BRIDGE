package com.paymentgateway.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.paymentgateway.bridge.abstraction.PaymentView;
import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.ui.components.GradientPanel;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.List;

/**
 * Main application window: header with gateway selector, one tab per view, status footer.
 * <p>
 * It only works with the abstractions {@link PaymentView} and {@link PaymentGatewayAPI};
 * concrete classes are created in {@code App} (the composition root).
 */
public class MainFrame extends JFrame {

    private final List<PaymentView> views;
    private final String invoiceProviderName;
    private final JLabel footerLabel = new JLabel();

    public MainFrame(List<PaymentGatewayAPI> gateways, List<PaymentView> views, String invoiceProviderName) {
        super("Enterprise Payment & Invoice Gateway");
        if (gateways.isEmpty() || views.isEmpty()) {
            throw new IllegalArgumentException("At least one gateway and one view are required.");
        }
        this.views = List.copyOf(views);
        this.invoiceProviderName = invoiceProviderName;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildHeader(gateways), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        updateFooter(gateways.get(0));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1000, screen.width), Math.min(680, screen.height)));
        setSize(Math.min(1280, screen.width), Math.min(820, screen.height));
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader(List<PaymentGatewayAPI> gateways) {
        JLabel title = new JLabel("Payment & Invoice Gateway");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        title.putClientProperty(FlatClientProperties.STYLE, "foreground: #FFFFFF");
        JLabel subtitle = new JLabel("Bridge pattern: views ↔ gateways   •   Adapter pattern: modern API ↔ legacy SOAP");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground: #E0E7FF");

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);

        JComboBox<PaymentGatewayAPI> gatewaySelector = new JComboBox<>(gateways.toArray(PaymentGatewayAPI[]::new));
        gatewaySelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = value instanceof PaymentGatewayAPI g ? g.getProviderName() : String.valueOf(value);
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        gatewaySelector.setPreferredSize(new Dimension(160, gatewaySelector.getPreferredSize().height));
        gatewaySelector.addActionListener(e -> switchGateway((PaymentGatewayAPI) gatewaySelector.getSelectedItem()));

        JToggleButton darkModeToggle = new JToggleButton("Dark mode");
        darkModeToggle.addActionListener(e -> AppTheme.setDarkMode(darkModeToggle.isSelected()));

        JLabel gatewayLabel = new JLabel("Payment gateway:");
        gatewayLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: #FFFFFF; font: bold");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(gatewayLabel);
        controls.add(gatewaySelector);
        controls.add(darkModeToggle);

        // Centers the controls vertically next to the two-line title.
        JPanel controlsHolder = new JPanel(new GridBagLayout());
        controlsHolder.setOpaque(false);
        controlsHolder.add(controls);

        GradientPanel header = new GradientPanel(new BorderLayout(), AppTheme.GRADIENT_START, AppTheme.GRADIENT_END, 0);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));
        header.add(titles, BorderLayout.WEST);
        header.add(controlsHolder, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_AREA_INSETS, new java.awt.Insets(0, 16, 0, 0));
        for (PaymentView view : views) {
            tabs.addTab(view.getViewTitle(), view);
        }
        return tabs;
    }

    private JPanel buildFooter() {
        footerLabel.putClientProperty(FlatClientProperties.STYLE, AppTheme.MUTED_LABEL_STYLE);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 8));
        footer.add(footerLabel);
        return footer;
    }

    /** Bridge in action: the same view objects now talk to a different implementation. */
    private void switchGateway(PaymentGatewayAPI gateway) {
        views.forEach(view -> view.setGateway(gateway));
        updateFooter(gateway);
    }

    private void updateFooter(PaymentGatewayAPI gateway) {
        footerLabel.setText("●  Active gateway: " + gateway.getProviderName()
                + "   |   Currencies: " + String.join(", ", gateway.getSupportedCurrencies())
                + "   |   Invoice provider: " + invoiceProviderName);
    }
}

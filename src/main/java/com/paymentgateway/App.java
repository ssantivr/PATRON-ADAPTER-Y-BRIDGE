package com.paymentgateway;

import com.paymentgateway.adapter.LegacyInvoiceAdapter;
import com.paymentgateway.bridge.abstraction.AdvancedInvoiceView;
import com.paymentgateway.bridge.abstraction.PaymentDashboard;
import com.paymentgateway.bridge.abstraction.PaymentView;
import com.paymentgateway.bridge.implementation.PayPalImplementation;
import com.paymentgateway.bridge.implementation.PaymentGatewayAPI;
import com.paymentgateway.bridge.implementation.StripeImplementation;
import com.paymentgateway.legacy.LegacySoapInvoiceService;
import com.paymentgateway.ui.MainFrame;
import com.paymentgateway.ui.theme.AppTheme;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Application entry point and composition root.
 * This is the only place that knows the concrete classes and wires them together.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.install();

            // Bridge: implementations
            List<PaymentGatewayAPI> gateways = List.of(new StripeImplementation(), new PayPalImplementation());
            PaymentGatewayAPI defaultGateway = gateways.get(0);

            // Adapter: legacy SOAP service exposed as a ModernInvoiceProvider
            LegacyInvoiceAdapter invoiceAdapter = new LegacyInvoiceAdapter(new LegacySoapInvoiceService());

            // Bridge: abstractions
            AdvancedInvoiceView invoiceView = new AdvancedInvoiceView(defaultGateway, invoiceAdapter);
            invoiceAdapter.setStageListener(invoiceView::onAdapterStage);
            List<PaymentView> views = List.of(new PaymentDashboard(defaultGateway), invoiceView);

            new MainFrame(gateways, views, invoiceAdapter.getProviderName()).setVisible(true);
        });
    }
}

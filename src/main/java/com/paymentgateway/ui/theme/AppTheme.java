package com.paymentgateway.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

/**
 * Central place for colors and Look &amp; Feel setup (FlatLaf).
 */
public final class AppTheme {

    public static final Color ACCENT = new Color(0x4F46E5);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color ERROR = new Color(0xDC2626);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color NEUTRAL = new Color(0x64748B);
    public static final Color VIOLET = new Color(0x9333EA);
    public static final Color GRADIENT_START = new Color(0x4F46E5);
    public static final Color GRADIENT_END = new Color(0xA21CAF);

    /** FlatLaf style for small uppercase captions such as "BILLED TO". */
    public static final String CAPTION_STYLE = "foreground: $Label.disabledForeground; font: bold -2";

    /** FlatLaf style for primary action buttons. */
    public static final String PRIMARY_BUTTON_STYLE =
            "background: #4F46E5; foreground: #FFFFFF; hoverBackground: #4338CA; "
                    + "pressedBackground: #3730A3; font: bold; arc: 12; borderWidth: 0; focusWidth: 0";

    /** FlatLaf style for secondary / muted labels. */
    public static final String MUTED_LABEL_STYLE = "foreground: $Label.disabledForeground";

    private AppTheme() {
    }

    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#4F46E5"));
        FlatLightLaf.setup();
        applyCommonDefaults();
    }

    public static void setDarkMode(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        applyCommonDefaults();
        FlatLaf.updateUI();
    }

    public static boolean isDarkMode() {
        return FlatLaf.isLafDark();
    }

    private static void applyCommonDefaults() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.selectedBackground", null);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
    }
}

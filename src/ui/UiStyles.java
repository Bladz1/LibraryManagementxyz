package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public final class UiStyles {
    private UiStyles() {}


    // Colors (approx to your mockup)
    public static final Color BG = new Color(248, 250, 255);
    public static final Color SIDEBAR_BG = new Color(245, 247, 255);
    public static final Color SIDEBAR_BORDER = new Color(229, 231, 235);
    public static final Color SIDEBAR_ACTIVE = new Color(99, 102, 241, 40);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color PRIMARY = new Color(99, 102, 241); // indigo-500
    public static final Color PRIMARY_DARK = new Color(79, 70, 229);
    public static final Color INFO = new Color(59, 130, 246);
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color WARNING = new Color(249, 115, 22);
    public static final Color TEXT = new Color(30, 41, 59);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color TABLE_ALT_ROW = new Color(248, 250, 252);
    public static final Color TABLE_SELECTION_BG = new Color(99, 102, 241, 40);


    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font H2 = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);


    private static final Border CELL_PADDING = new EmptyBorder(0, 12, 0, 12);

    public static Panel card(int width, int height) {
        Panel p = new Panel();
        p.setBackground(CARD_BG);
        p.setPreferredSize(new Dimension(width, height));
        p.setLayout(new BorderLayout());
        p.setFont(BODY);
        p.setForeground(Color.DARK_GRAY);
        p.setName("card");
        return p;
    }

    public static JPanel surface(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        styleSurface(panel);
        return panel;
    }

    public static void styleSurface(JComponent component) {
        component.setOpaque(true);
        component.setBackground(CARD_BG);
        component.setBorder(new EmptyBorder(16, 16, 16, 16));
        component.putClientProperty("JComponent.roundRect", true);
    }

    public static void styleSearchField(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.putClientProperty("JComponent.roundRect", true);
    }

    public static void stylePrimaryButton(JButton button) {
        baseButton(button);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
    }

    public static void styleSuccessButton(JButton button) {
        baseButton(button);
        button.setBackground(SUCCESS);
        button.setForeground(Color.WHITE);
    }

    public static void styleWarningButton(JButton button) {
        baseButton(button);
        button.setBackground(WARNING);
        button.setForeground(Color.WHITE);
    }

    public static void styleDangerButton(JButton button) {
        baseButton(button);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
    }

    private static void baseButton(JButton button) {
        button.setFont(H2.deriveFont(Font.PLAIN, 14f));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JButton.arc", 18);
        button.putClientProperty("JComponent.roundRect", true);
        button.setIconTextGap(8);
    }

    public static void styleGhostButton(JButton button) {
        baseButton(button);
        button.setBackground(new Color(255, 255, 255, 45));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 120), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
    }

    public static void applyTableStyling(JTable table) {
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setRowHeight(32);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(TABLE_SELECTION_BG);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(H2.deriveFont(Font.PLAIN, 13f));
            header.setBackground(new Color(241, 245, 249));
            header.setForeground(TEXT);
            header.setReorderingAllowed(false);
            TableCellRenderer headerRenderer = header.getDefaultRenderer();
            if (headerRenderer instanceof DefaultTableCellRenderer renderer) {
                renderer.setHorizontalAlignment(SwingConstants.LEFT);
                renderer.setBorder(new EmptyBorder(8, 12, 8, 12));
            }
        }

        table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER),
                new EmptyBorder(0, 0, 0, 0)
        ));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static class StripedTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setBackground(TABLE_SELECTION_BG);
                c.setForeground(TEXT);
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_ALT_ROW);
                c.setForeground(TEXT);
            }
            if (c instanceof JComponent component) {
                component.setBorder(CELL_PADDING);
            }
            return c;
        }
    }
}

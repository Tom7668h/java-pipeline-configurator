package de.tom.configurator.ui.components;

import de.tom.configurator.model.ScanStatus;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom table cell renderer drawing modern status badges for scan lifecycle states.
 */
public class StatusBadgeCellRenderer extends DefaultTableCellRenderer {

    private static final Color COLOR_PROCESSED_BG = new Color(20, 70, 45);
    private static final Color COLOR_PROCESSED_FG = new Color(116, 225, 155);

    private static final Color COLOR_QUARANTINE_BG = new Color(92, 61, 0);
    private static final Color COLOR_QUARANTINE_FG = new Color(255, 209, 102);

    private static final Color COLOR_REJECTED_BG = new Color(85, 20, 30);
    private static final Color COLOR_REJECTED_FG = new Color(255, 125, 145);

    private static final Color COLOR_DEFAULT_BG = new Color(45, 55, 72);
    private static final Color COLOR_DEFAULT_FG = new Color(200, 210, 220);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setOpaque(true);

        if (value instanceof ScanStatus status) {
            configureForStatus(label, status);
        } else if (value != null) {
            try {
                ScanStatus status = ScanStatus.valueOf(value.toString().trim());
                configureForStatus(label, status);
            } catch (IllegalArgumentException e) {
                label.setBackground(COLOR_DEFAULT_BG);
                label.setForeground(COLOR_DEFAULT_FG);
                label.setText(value.toString());
            }
        } else {
            label.setText("-");
        }

        return label;
    }

    private void configureForStatus(JLabel label, ScanStatus status) {
        switch (status) {
            case PROCESSED -> {
                label.setBackground(COLOR_PROCESSED_BG);
                label.setForeground(COLOR_PROCESSED_FG);
                label.setText("✔ PROCESSED");
            }
            case QUARANTINE -> {
                label.setBackground(COLOR_QUARANTINE_BG);
                label.setForeground(COLOR_QUARANTINE_FG);
                label.setText("⚠ QUARANTINE");
            }
            case REJECTED -> {
                label.setBackground(COLOR_REJECTED_BG);
                label.setForeground(COLOR_REJECTED_FG);
                label.setText("✖ REJECTED");
            }
            case PENDING -> {
                label.setBackground(COLOR_DEFAULT_BG);
                label.setForeground(COLOR_DEFAULT_FG);
                label.setText("⏳ PENDING");
            }
        }
    }
}

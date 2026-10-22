package de.tom.configurator.ui.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Modern KPI telemetry card component for dashboard metrics.
 */
public class MetricCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public MetricCard(String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(30, 36, 48));
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(45, 55, 72), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        // Header: Title
        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(new Color(156, 163, 175));

        // Center: Value
        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor != null ? accentColor : Color.WHITE);

        // Footer: Subtitle
        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        contentPanel.setOpaque(false);
        contentPanel.add(valueLabel);
        contentPanel.add(subtitleLabel);

        add(titleLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void updateValue(String value) {
        SwingUtilities.invokeLater(() -> valueLabel.setText(value));
    }

    public void updateSubtitle(String subtitle) {
        SwingUtilities.invokeLater(() -> subtitleLabel.setText(subtitle));
    }

    public void setValueColor(Color color) {
        SwingUtilities.invokeLater(() -> valueLabel.setForeground(color));
    }
}

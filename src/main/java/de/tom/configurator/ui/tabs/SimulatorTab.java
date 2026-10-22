package de.tom.configurator.ui.tabs;

import de.tom.configurator.model.ScanEvent;
import de.tom.configurator.simulator.SimulationController;
import de.tom.configurator.ui.components.MetricCard;
import de.tom.configurator.ui.components.StatusBadgeCellRenderer;
import de.tom.configurator.ui.event.ScanEventListener;
import java.awt.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Tab 2: Multi-Threaded Scanner Simulator with Live Telemetry Stream and KPI Cards.
 */
public class SimulatorTab extends JPanel implements ScanEventListener {

    private final SimulationController simulationController;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    // KPI Cards
    private MetricCard totalScansCard;
    private MetricCard throughputCard;
    private MetricCard quarantineRateCard;
    private MetricCard rejectedCard;

    // Controls
    private JButton startBtn;
    private JButton pauseBtn;
    private JSlider speedSlider;
    private JLabel speedLabel;
    private JCheckBox autoScrollCheckBox;

    // Table
    private DefaultTableModel streamTableModel;
    private JTable streamTable;
    private static final int MAX_TABLE_ROWS = 250;

    public SimulatorTab(SimulationController simulationController) {
        this.simulationController = simulationController;
        this.simulationController.addScanEventListener(this);

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top: KPI Cards
        add(createKpiPanel(), BorderLayout.NORTH);

        // Center: Controls + Stream Table
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(createControlBar(), BorderLayout.NORTH);
        centerPanel.add(createTablePanel(), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createKpiPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));

        totalScansCard = new MetricCard("Total Scans", "0", "Echtzeit verarbeitet", new Color(100, 180, 255));
        throughputCard = new MetricCard("Taktfrequenz", "120 / min", "500 ms Intervall", new Color(180, 140, 255));
        quarantineRateCard = new MetricCard("Quarantäne-Quote", "0.0 %", "Gewichtsabweichung > 15%", new Color(255, 209, 102));
        rejectedCard = new MetricCard("Abgelehnt / Fehler", "0", "Syntax / Unbekannte SKU", new Color(255, 125, 145));

        panel.add(totalScansCard);
        panel.add(throughputCard);
        panel.add(quarantineRateCard);
        panel.add(rejectedCard);

        return panel;
    }

    private JPanel createControlBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 5));
        bar.setBackground(new Color(24, 28, 38));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 55, 72), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Left: Start / Pause / Anomaly Buttons
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);

        startBtn = new JButton("▶ Start Simulation");
        startBtn.setBackground(new Color(20, 110, 60));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFont(startBtn.getFont().deriveFont(Font.BOLD));
        startBtn.addActionListener(e -> startSimulation());

        pauseBtn = new JButton("⏸ Pause");
        pauseBtn.setEnabled(false);
        pauseBtn.addActionListener(e -> pauseSimulation());

        JButton anomalyWeightBtn = new JButton("⚡ Gewichts-Sprung (+40%)");
        anomalyWeightBtn.setToolTipText("Injected sofort einen Barcode mit unzulässiger Gewichtsabweichung");
        anomalyWeightBtn.addActionListener(e -> simulationController.injectManualAnomaly("WEIGHT_DIVERGENCE"));

        JButton anomalyUnknownSkuBtn = new JButton("❓ Unbekannte SKU");
        anomalyUnknownSkuBtn.addActionListener(e -> simulationController.injectManualAnomaly("UNKNOWN_SKU"));

        JButton anomalyCorruptBtn = new JButton("💥 Syntax-Fehler");
        anomalyCorruptBtn.addActionListener(e -> simulationController.injectManualAnomaly("CORRUPT_SYNTAX"));

        leftActions.add(startBtn);
        leftActions.add(pauseBtn);
        leftActions.add(new JSeparator(SwingConstants.VERTICAL));
        leftActions.add(new JLabel("Manuelle Anomalie-Injektion:"));
        leftActions.add(anomalyWeightBtn);
        leftActions.add(anomalyUnknownSkuBtn);
        leftActions.add(anomalyCorruptBtn);

        // Right: Speed Slider & Auto-scroll
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        speedLabel = new JLabel("Frequenz: 500 ms");
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        speedLabel.setForeground(new Color(156, 163, 175));

        speedSlider = new JSlider(50, 1500, 500);
        speedSlider.setPreferredSize(new Dimension(130, 24));
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedLabel.setText("Frequenz: " + val + " ms");
            throughputCard.updateSubtitle(val + " ms Intervall");
            throughputCard.updateValue((60000 / val) + " / min");
            simulationController.updateFrequency(val);
        });

        autoScrollCheckBox = new JCheckBox("Auto-Scroll", true);
        autoScrollCheckBox.setOpaque(false);

        JButton clearBtn = new JButton("Tabelle leeren");
        clearBtn.addActionListener(e -> {
            streamTableModel.setRowCount(0);
            simulationController.resetCounters();
            updateKpiWidgets();
        });

        rightActions.add(speedLabel);
        rightActions.add(speedSlider);
        rightActions.add(autoScrollCheckBox);
        rightActions.add(clearBtn);

        bar.add(leftActions, BorderLayout.WEST);
        bar.add(rightActions, BorderLayout.EAST);

        return bar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] columns = {"Zeit", "Station", "Barcode Payload", "SKU / Baugruppe", "Gewicht (Ist / Soll)", "Status", "Routing & Grund", "Latenz"};
        streamTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        streamTable = new JTable(streamTableModel);
        streamTable.setRowHeight(24);
        streamTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        streamTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        streamTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        streamTable.getColumnModel().getColumn(0).setPreferredWidth(95);  // Zeit
        streamTable.getColumnModel().getColumn(1).setPreferredWidth(140); // Station
        streamTable.getColumnModel().getColumn(2).setPreferredWidth(260); // Payload
        streamTable.getColumnModel().getColumn(3).setPreferredWidth(130); // SKU
        streamTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Gewicht
        streamTable.getColumnModel().getColumn(5).setPreferredWidth(115); // Status
        streamTable.getColumnModel().getColumn(6).setPreferredWidth(290); // Grund
        streamTable.getColumnModel().getColumn(7).setPreferredWidth(60);  // Latenz

        // Custom Status Badge Renderer
        streamTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeCellRenderer());

        JScrollPane scrollPane = new JScrollPane(streamTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startSimulation() {
        startBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        simulationController.startSimulation(speedSlider.getValue());
    }

    private void pauseSimulation() {
        startBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        simulationController.pauseSimulation();
    }

    @Override
    public void onScanProcessed(ScanEvent event) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(event.getScannedAt());

            String weightStr = "-";
            if (event.getExtractedWeightG() != null) {
                if (event.getExpectedWeightG() != null) {
                    weightStr = event.getExtractedWeightG() + " g (" + event.getExpectedWeightG() + " g)";
                } else {
                    weightStr = event.getExtractedWeightG() + " g";
                }
            }

            streamTableModel.addRow(new Object[]{
                    timeStr,
                    event.getStationId(),
                    event.getRawPayload(),
                    event.getExtractedSku() != null ? event.getExtractedSku() : "-",
                    weightStr,
                    event.getStatus(),
                    event.getStatusReason(),
                    event.getProcessingTimeMs() + " ms"
            });

            // Trim table rows if exceeding buffer
            if (streamTableModel.getRowCount() > MAX_TABLE_ROWS) {
                streamTableModel.removeRow(0);
            }

            // Auto-scroll to bottom
            if (autoScrollCheckBox.isSelected()) {
                int lastRow = streamTable.getRowCount() - 1;
                if (lastRow >= 0) {
                    streamTable.scrollRectToVisible(streamTable.getCellRect(lastRow, 0, true));
                }
            }

            updateKpiWidgets();
        });
    }

    private void updateKpiWidgets() {
        long total = simulationController.getTotalScans();
        long processed = simulationController.getProcessedCount();
        long quarantined = simulationController.getQuarantineCount();
        long rejected = simulationController.getRejectedCount();

        totalScansCard.updateValue(String.valueOf(total));
        totalScansCard.updateSubtitle(processed + " erfolgreich verbucht");

        double qRate = simulationController.getQuarantineRatePct();
        quarantineRateCard.updateValue(String.format("%.1f %%", qRate));
        quarantineRateCard.updateSubtitle(quarantined + " Pakete in Quarantäne");

        rejectedCard.updateValue(String.valueOf(rejected));
        rejectedCard.updateSubtitle("Ausschuss / Fehlscans");
    }
}

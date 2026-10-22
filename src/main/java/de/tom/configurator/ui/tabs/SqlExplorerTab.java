package de.tom.configurator.ui.tabs;

import de.tom.configurator.service.DatabaseQueryService;
import de.tom.configurator.ui.components.StatusBadgeCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Tab 3: Dynamic SQL Database Explorer with Interactive Query Console and CSV Exporter.
 */
public class SqlExplorerTab extends JPanel {

    private final DatabaseQueryService queryService;

    private JComboBox<String> tableSelector;
    private JTextArea sqlEditor;
    private JLabel statusLabel;
    private JTable resultsTable;
    private DefaultTableModel tableModel;

    private Vector<String> currentColumns = new Vector<>();
    private Vector<Vector<Object>> currentData = new Vector<>();

    public SqlExplorerTab(DatabaseQueryService queryService) {
        this.queryService = queryService;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top: Quick Table Navigation + SQL Editor
        JPanel topContainer = new JPanel(new BorderLayout(6, 6));
        topContainer.add(createToolbar(), BorderLayout.NORTH);
        topContainer.add(createSqlConsolePanel(), BorderLayout.CENTER);

        // Center: Dynamic Result Table
        JPanel resultsPanel = createResultsPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContainer, resultsPanel);
        splitPane.setResizeWeight(0.35);
        add(splitPane, BorderLayout.CENTER);

        // Initial default query
        executeSql("SELECT * FROM scan_logs ORDER BY log_id DESC LIMIT 50;");
    }

    private JPanel createToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bar.setBackground(new Color(24, 28, 38));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 55, 72), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel label = new JLabel("Schnellauswahl Tabelle:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));

        List<String> tables = queryService.getUserTableNames();
        tableSelector = new JComboBox<>(tables.toArray(new String[0]));
        tableSelector.setSelectedItem("SCAN_LOGS");

        JButton loadTableBtn = new JButton("Tabelle laden");
        loadTableBtn.addActionListener(e -> {
            String selected = (String) tableSelector.getSelectedItem();
            if (selected != null) {
                sqlEditor.setText("SELECT * FROM " + selected + " ORDER BY 1 DESC LIMIT 100;");
                executeSql(sqlEditor.getText());
            }
        });

        JButton exportCsvBtn = new JButton("📥 Als CSV Exportieren");
        exportCsvBtn.setBackground(new Color(40, 70, 100));
        exportCsvBtn.setForeground(Color.WHITE);
        exportCsvBtn.addActionListener(e -> exportToCsv());

        bar.add(label);
        bar.add(tableSelector);
        bar.add(loadTableBtn);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(exportCsvBtn);

        return bar;
    }

    private JPanel createSqlConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                " SQL Query Console (H2 & MySQL Dialect) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(180, 200, 230)
        ));

        sqlEditor = new JTextArea("SELECT * FROM scan_logs ORDER BY log_id DESC LIMIT 50;");
        sqlEditor.setFont(new Font("Consolas", Font.PLAIN, 13));
        sqlEditor.setBackground(new Color(20, 24, 33));
        sqlEditor.setForeground(new Color(230, 235, 245));
        sqlEditor.setCaretColor(Color.WHITE);
        sqlEditor.setRows(4);

        JScrollPane scrollPane = new JScrollPane(sqlEditor);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        statusLabel = new JLabel("Bereit");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(156, 163, 175));

        JButton executeBtn = new JButton("▶ Ausführen");
        executeBtn.setBackground(new Color(20, 110, 60));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFont(executeBtn.getFont().deriveFont(Font.BOLD));
        executeBtn.addActionListener(e -> executeSql(sqlEditor.getText()));

        actionPanel.add(statusLabel);
        actionPanel.add(executeBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                " Abfrage-Ergebnisse (Dynamic Data Grid) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(116, 225, 155)
        ));

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(22);
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(resultsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public void executeSql(String sql) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("Führe Query aus...");

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseQueryService.QueryResult result = queryService.executeQuery(sql);

                if (result.isSuccess()) {
                    currentColumns = result.getColumnNames();
                    currentData = result.getData();

                    tableModel.setDataVector(currentData, currentColumns);

                    // Resize columns to fit contents
                    for (int i = 0; i < resultsTable.getColumnCount(); i++) {
                        String colName = resultsTable.getColumnName(i);
                        if ("STATUS".equalsIgnoreCase(colName)) {
                            resultsTable.getColumnModel().getColumn(i).setCellRenderer(new StatusBadgeCellRenderer());
                            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(120);
                        } else if ("RAW_PAYLOAD".equalsIgnoreCase(colName) || "STATUS_REASON".equalsIgnoreCase(colName)) {
                            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(250);
                        } else {
                            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(130);
                        }
                    }

                    statusLabel.setText("✔ " + result.getMessage() + " (" + result.getExecutionTimeMs() + " ms)");
                    statusLabel.setForeground(new Color(116, 225, 155));
                } else {
                    statusLabel.setText("✖ " + result.getMessage());
                    statusLabel.setForeground(new Color(255, 125, 145));
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void exportToCsv() {
        if (currentColumns.isEmpty() || currentData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Daten zum Exportieren vorhanden.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportiere Query-Ergebnisse als CSV");
        fileChooser.setSelectedFile(new File("sql_export_" + System.currentTimeMillis() + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                queryService.exportToCsv(fileToSave, currentColumns, currentData);
                JOptionPane.showMessageDialog(this, "Export erfolgreich gespeichert nach:\n" + fileToSave.getAbsolutePath(), "Export Abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim CSV-Export: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

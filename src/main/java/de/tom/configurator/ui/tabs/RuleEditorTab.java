package de.tom.configurator.ui.tabs;

import de.tom.configurator.dao.RoutingRuleDao;
import de.tom.configurator.model.RoutingRule;
import de.tom.configurator.service.RuleParserService;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Tab 1: Visual Barcode Routing Rule Editor with Real-Time Live Regex Testing.
 */
public class RuleEditorTab extends JPanel {

    private final RoutingRuleDao routingRuleDao;
    private final RuleParserService parserService;

    private JTable rulesTable;
    private DefaultTableModel tableModel;

    // Form fields
    private JTextField nameField;
    private JTextField regexField;
    private JComboBox<String> stationCombo;
    private JComboBox<String> actionCombo;
    private JSpinner prioritySpinner;
    private JCheckBox activeCheckBox;
    private Integer selectedRuleId = null;

    // Live Regex Tester fields
    private JTextField testBarcodeField;
    private JLabel testResultStatus;
    private JLabel testResultSku;
    private JLabel testResultLot;
    private JLabel testResultWeight;
    private JLabel testResultRule;

    public RuleEditorTab(RoutingRuleDao routingRuleDao, RuleParserService parserService) {
        this.routingRuleDao = routingRuleDao;
        this.parserService = parserService;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top: Split between Table and Form
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setTopComponent(createTablePanel());
        splitPane.setBottomComponent(createBottomPanel());

        add(splitPane, BorderLayout.CENTER);

        loadRules();
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                " Aktive Hardware Routing-Regeln ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(180, 200, 230)
        ));

        String[] columns = {"ID", "Regelname", "Regex Pattern", "Station", "Ziel-Aktion", "Priorität", "Aktiv"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 5) return Integer.class;
                if (columnIndex == 6) return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only 'Aktiv' checkbox editable directly
            }
        };

        rulesTable = new JTable(tableModel);
        rulesTable.setRowHeight(26);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rulesTable.getTableHeader().setReorderingAllowed(false);

        // Listen for checkbox toggle
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 6 && e.getFirstRow() >= 0) {
                int row = e.getFirstRow();
                Integer ruleId = (Integer) tableModel.getValueAt(row, 0);
                Boolean isActive = (Boolean) tableModel.getValueAt(row, 6);
                if (ruleId != null && isActive != null) {
                    routingRuleDao.toggleActive(ruleId, isActive);
                }
            }
        });

        // Listen for row selection to populate edit form
        rulesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && rulesTable.getSelectedRow() >= 0) {
                populateFormFromSelectedRow(rulesTable.getSelectedRow());
            }
        });

        JScrollPane scrollPane = new JScrollPane(rulesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Table Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton refreshBtn = new JButton("🔄 Aktualisieren");
        refreshBtn.addActionListener(e -> loadRules());

        JButton newBtn = new JButton("➕ Neue Regel");
        newBtn.addActionListener(e -> clearForm());

        JButton deleteBtn = new JButton("🗑 Regel löschen");
        deleteBtn.addActionListener(e -> deleteSelectedRule());

        toolbar.add(refreshBtn);
        toolbar.add(newBtn);
        toolbar.add(deleteBtn);
        panel.add(toolbar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.add(createFormPanel());
        panel.add(createLiveTesterPanel());
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                " Regel Details & Editor ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(180, 200, 230)
        ));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.weightx = 1.0;

        nameField = new JTextField();
        regexField = new JTextField();
        stationCombo = new JComboBox<>(new String[]{"ALL", "STATION_01_INBOUND", "STATION_02_SORTING", "STATION_03_ASSEMBLY", "STATION_04_DISPATCH"});
        actionCombo = new JComboBox<>(new String[]{"INVENTORY_BOOKING", "QUALITY_CHECK", "DISPATCH", "QUARANTINE_HOLD"});
        prioritySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        activeCheckBox = new JCheckBox("Regel ist aktiv", true);

        int row = 0;
        addFormField(formGrid, gbc, row++, "Regel-Name:", nameField);
        addFormField(formGrid, gbc, row++, "Regex Pattern:", regexField);
        addFormField(formGrid, gbc, row++, "Station ID:", stationCombo);
        addFormField(formGrid, gbc, row++, "Ziel-Aktion:", actionCombo);
        addFormField(formGrid, gbc, row++, "Priorität (1=höchste):", prioritySpinner);

        gbc.gridx = 1;
        gbc.gridy = row++;
        formGrid.add(activeCheckBox, gbc);

        panel.add(formGrid, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton saveBtn = new JButton("💾 Regel Speichern");
        saveBtn.setBackground(new Color(30, 90, 150));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveCurrentRule());

        JButton clearBtn = new JButton("Zurücksetzen");
        clearBtn.addActionListener(e -> clearForm());

        buttonPanel.add(clearBtn);
        buttonPanel.add(saveBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }

    private JPanel createLiveTesterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                " 🔬 Live-Regex Tester & Gruppen-Extraktor ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(116, 225, 155)
        ));

        JPanel topInputPanel = new JPanel(new BorderLayout(5, 5));
        topInputPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel infoLabel = new JLabel("Test-Barcode (Echtzeit-Extraktion der Named Groups: sku, lot, weight):");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(156, 163, 175));

        testBarcodeField = new JTextField("010401234510001810LOT8819A310000450");
        testBarcodeField.setFont(new Font("Consolas", Font.PLAIN, 13));

        topInputPanel.add(infoLabel, BorderLayout.NORTH);
        topInputPanel.add(testBarcodeField, BorderLayout.CENTER);

        // Results grid
        JPanel resultsGrid = new JPanel(new GridLayout(5, 2, 4, 6));
        resultsGrid.setBorder(new EmptyBorder(8, 8, 8, 8));
        resultsGrid.setBackground(new Color(24, 28, 38));

        testResultStatus = createBadgeLabel("WARTE AUF EINGABE", Color.GRAY);
        testResultSku = new JLabel("-");
        testResultLot = new JLabel("-");
        testResultWeight = new JLabel("-");
        testResultRule = new JLabel("-");

        testResultSku.setFont(new Font("Consolas", Font.BOLD, 12));
        testResultLot.setFont(new Font("Consolas", Font.BOLD, 12));
        testResultWeight.setFont(new Font("Consolas", Font.BOLD, 12));

        addResultRow(resultsGrid, "Match Status:", testResultStatus);
        addResultRow(resultsGrid, "Extrahierte SKU:", testResultSku);
        addResultRow(resultsGrid, "Extrahierte Charge (lot):", testResultLot);
        addResultRow(resultsGrid, "Extr. Gewicht (weight):", testResultWeight);
        addResultRow(resultsGrid, "Gematchte Regel:", testResultRule);

        panel.add(topInputPanel, BorderLayout.NORTH);
        panel.add(resultsGrid, BorderLayout.CENTER);

        // Document Listener for Live-Testing
        testBarcodeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { runLiveTest(); }
            @Override
            public void removeUpdate(DocumentEvent e) { runLiveTest(); }
            @Override
            public void changedUpdate(DocumentEvent e) { runLiveTest(); }
        });

        // Trigger initial test
        SwingUtilities.invokeLater(this::runLiveTest);

        return panel;
    }

    private void addResultRow(JPanel panel, String labelText, JComponent comp) {
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(156, 163, 175));
        panel.add(label);
        panel.add(comp);
    }

    private JLabel createBadgeLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(color);
        return label;
    }

    private void runLiveTest() {
        String testBarcode = testBarcodeField.getText().trim();
        if (testBarcode.isEmpty()) {
            testResultStatus.setText("LEER");
            testResultStatus.setForeground(Color.GRAY);
            testResultSku.setText("-");
            testResultLot.setText("-");
            testResultWeight.setText("-");
            testResultRule.setText("-");
            return;
        }

        List<RoutingRule> activeRules = routingRuleDao.findActiveRulesSorted();
        Optional<RuleParserService.ParseResult> matchOpt = parserService.matchAndParse(null, testBarcode, activeRules);

        if (matchOpt.isPresent()) {
            RuleParserService.ParseResult res = matchOpt.get();
            testResultStatus.setText("✔ MATCH SUCCESS");
            testResultStatus.setForeground(new Color(116, 225, 155));
            testResultSku.setText(res.getSku() != null ? res.getSku() : "(nicht definiert)");
            testResultLot.setText(res.getLot() != null ? res.getLot() : "(nicht definiert)");
            testResultWeight.setText(res.getWeightG() != null ? res.getWeightG() + " g" : "(nicht definiert)");
            testResultRule.setText(res.getMatchedRule().getName() + " [Prio " + res.getMatchedRule().getPriority() + "]");
        } else {
            testResultStatus.setText("✖ KEIN MATCH");
            testResultStatus.setForeground(new Color(255, 125, 145));
            testResultSku.setText("-");
            testResultLot.setText("-");
            testResultWeight.setText("-");
            testResultRule.setText("Keine aktive Regel passt auf diese Syntax");
        }
    }

    public void loadRules() {
        tableModel.setRowCount(0);
        List<RoutingRule> rules = routingRuleDao.findAll();
        for (RoutingRule r : rules) {
            tableModel.addRow(new Object[]{
                    r.getRuleId(),
                    r.getName(),
                    r.getRegexPattern(),
                    r.getStationId(),
                    r.getTargetAction(),
                    r.getPriority(),
                    r.isActive()
            });
        }
        runLiveTest();
    }

    private void populateFormFromSelectedRow(int row) {
        selectedRuleId = (Integer) tableModel.getValueAt(row, 0);
        nameField.setText((String) tableModel.getValueAt(row, 1));
        regexField.setText((String) tableModel.getValueAt(row, 2));
        stationCombo.setSelectedItem(tableModel.getValueAt(row, 3));
        actionCombo.setSelectedItem(tableModel.getValueAt(row, 4));
        prioritySpinner.setValue(tableModel.getValueAt(row, 5));
        activeCheckBox.setSelected((Boolean) tableModel.getValueAt(row, 6));
    }

    private void clearForm() {
        selectedRuleId = null;
        rulesTable.clearSelection();
        nameField.setText("");
        regexField.setText("");
        stationCombo.setSelectedIndex(0);
        actionCombo.setSelectedIndex(0);
        prioritySpinner.setValue(10);
        activeCheckBox.setSelected(true);
    }

    private void saveCurrentRule() {
        String name = nameField.getText().trim();
        String regex = regexField.getText().trim();

        if (name.isEmpty() || regex.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte Name und Regex-Pattern angeben!", "Eingabefehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RoutingRule rule = new RoutingRule(
                selectedRuleId,
                name,
                regex,
                (String) stationCombo.getSelectedItem(),
                (String) actionCombo.getSelectedItem(),
                (Integer) prioritySpinner.getValue(),
                activeCheckBox.isSelected()
        );

        boolean success = routingRuleDao.saveOrUpdate(rule);
        if (success) {
            loadRules();
            clearForm();
            JOptionPane.showMessageDialog(this, "Regel erfolgreich gespeichert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern der Regel in die Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Regel zum Löschen aus.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int ruleId = (Integer) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie Regel #" + ruleId + " wirklich löschen?", "Bestätigung", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            routingRuleDao.deleteById(ruleId);
            loadRules();
            clearForm();
        }
    }
}

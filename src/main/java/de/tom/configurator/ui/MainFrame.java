package de.tom.configurator.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import de.tom.configurator.dao.RoutingRuleDao;
import de.tom.configurator.service.DatabaseQueryService;
import de.tom.configurator.service.PipelineProcessor;
import de.tom.configurator.service.RuleParserService;
import de.tom.configurator.simulator.SimulationController;
import de.tom.configurator.ui.tabs.RuleEditorTab;
import de.tom.configurator.ui.tabs.SimulatorTab;
import de.tom.configurator.ui.tabs.SqlExplorerTab;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Main Application Window with JetBrains-style FlatLaf Dark Look & Feel.
 */
public class MainFrame extends JFrame {

    private final PipelineProcessor pipelineProcessor;
    private final SimulationController simulationController;
    private final DatabaseQueryService queryService;

    public MainFrame(PipelineProcessor pipelineProcessor, SimulationController simulationController, DatabaseQueryService queryService) {
        this.pipelineProcessor = pipelineProcessor;
        this.simulationController = simulationController;
        this.queryService = queryService;

        initLookAndFeel();
        initComponents();
    }

    private void initLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.selectedBackground", new Color(40, 48, 64));
            UIManager.put("ScrollBar.thumbArc", 6);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("Button.arc", 6);
            UIManager.put("Component.arc", 6);
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf Dark theme: " + e.getMessage());
        }
    }

    private void initComponents() {
        setTitle("Industrial Hardware & SQL Pipeline Configurator • Java 17/21 LTS • FlatLaf Dark");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setPreferredSize(new Dimension(1280, 800));

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        RuleEditorTab ruleEditorTab = new RuleEditorTab(pipelineProcessor.getRoutingRuleDao(), pipelineProcessor.getParserService());
        SimulatorTab simulatorTab = new SimulatorTab(simulationController);
        SqlExplorerTab sqlExplorerTab = new SqlExplorerTab(queryService);

        tabbedPane.addTab("  📐 Regel-Editor & Live-Regex  ", ruleEditorTab);
        tabbedPane.addTab("  🏭 Hardware-Simulator & Scanner-Stream  ", simulatorTab);
        tabbedPane.addTab("  💾 SQL Database Explorer  ", sqlExplorerTab);

        // Switch to simulator tab when starting
        tabbedPane.setSelectedIndex(1);

        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        rootPanel.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 5));
        header.setBackground(new Color(18, 22, 30));
        header.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(40, 50, 68), 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("⚡");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Industrial Hardware & SQL Pipeline Configurator");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Multi-Threaded Scanner Simulation, Regex Routing & Dynamic SQL Storage Engine");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(156, 163, 175));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        brandPanel.add(logoLabel);
        brandPanel.add(textPanel);

        // Right Badges & Indicators
        JPanel indicatorsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        indicatorsPanel.setOpaque(false);

        JLabel dbStatus = createPill("● H2 SQL Database: Connected", new Color(20, 70, 45), new Color(116, 225, 155));
        JLabel poolStatus = createPill("HikariCP: Active (10 Conns)", new Color(24, 38, 60), new Color(100, 180, 255));
        JLabel stationStatus = createPill("4 Scanner-Stations Online", new Color(45, 35, 65), new Color(200, 150, 255));

        indicatorsPanel.add(dbStatus);
        indicatorsPanel.add(poolStatus);
        indicatorsPanel.add(stationStatus);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(indicatorsPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(15, 18, 24));
        footer.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel leftLabel = new JLabel("Produktionsreife Desktop-Architektur • Producer-Consumer Concurrency • Tom7668h");
        leftLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        leftLabel.setForeground(new Color(120, 130, 145));

        JLabel rightLabel = new JLabel("Java 17/21 LTS • FlatLaf 3.5 • HikariCP • JUnit 5");
        rightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightLabel.setForeground(new Color(120, 130, 145));

        footer.add(leftLabel, BorderLayout.WEST);
        footer.add(rightLabel, BorderLayout.EAST);

        return footer;
    }

    private JLabel createPill(String text, Color bg, Color fg) {
        JLabel pill = new JLabel(text);
        pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        pill.setBackground(bg);
        pill.setForeground(fg);
        pill.setOpaque(true);
        pill.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(fg.darker(), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        return pill;
    }
}

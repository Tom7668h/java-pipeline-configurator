package de.tom.configurator;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.service.DatabaseQueryService;
import de.tom.configurator.service.PipelineProcessor;
import de.tom.configurator.simulator.SimulationController;
import de.tom.configurator.ui.MainFrame;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Application Entry Point for Industrial Hardware & SQL Pipeline Configurator.
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Industrial Hardware & SQL Pipeline Configurator...");

        // Enable high DPI display scaling on modern Windows systems
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        // 1. Initialize Database & Schema
        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();

        // 2. Initialize Core Pipeline Services
        PipelineProcessor pipelineProcessor = new PipelineProcessor();
        SimulationController simulationController = new SimulationController(pipelineProcessor);
        DatabaseQueryService queryService = new DatabaseQueryService(databaseConfig);

        // 3. Register Shutdown Hook for Graceful Cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Industrial Hardware Pipeline Configurator...");
            simulationController.stopSimulation();
            databaseConfig.close();
            LOGGER.info("Shutdown completed cleanly.");
        }, "ShutdownHookThread"));

        // 4. Launch Swing GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame(pipelineProcessor, simulationController, queryService);
                frame.setVisible(true);
                LOGGER.info("MainFrame UI displayed successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to launch GUI", e);
                JOptionPane.showMessageDialog(null,
                        "Kritischer Fehler beim Starten der Oberfläche: " + e.getMessage(),
                        "Startfehler",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

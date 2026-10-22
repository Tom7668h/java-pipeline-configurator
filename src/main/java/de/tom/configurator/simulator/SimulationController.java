package de.tom.configurator.simulator;

import de.tom.configurator.model.ScanEvent;
import de.tom.configurator.service.PipelineProcessor;
import de.tom.configurator.ui.event.ScanEventListener;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for multi-threaded industrial hardware scan simulation.
 * Uses a thread pool to dispatch and process scanner reads with atomic throughput telemetry.
 */
public class SimulationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationController.class);

    private final PipelineProcessor pipelineProcessor;
    private final ScanEventGenerator generator;
    private final List<ScanEventListener> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> simulationTask;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalScansCounter = new AtomicLong(0);
    private final AtomicLong processedCounter = new AtomicLong(0);
    private final AtomicLong quarantineCounter = new AtomicLong(0);
    private final AtomicLong rejectedCounter = new AtomicLong(0);

    private double anomalyRate = 0.20;
    private int currentFrequencyMs = 500;

    public SimulationController(PipelineProcessor pipelineProcessor) {
        this.pipelineProcessor = pipelineProcessor;
        this.generator = new ScanEventGenerator();
    }

    public synchronized void startSimulation(int frequencyMs) {
        if (running.get()) {
            return;
        }

        this.currentFrequencyMs = Math.max(50, frequencyMs);
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "HardwareScannerThread");
            t.setDaemon(true);
            return t;
        });

        running.set(true);
        LOGGER.info("Starting hardware scanner simulation at {} ms frequency ({} scans/min)",
                currentFrequencyMs, (60000 / currentFrequencyMs));

        simulationTask = scheduler.scheduleAtFixedRate(this::tickScan, 100, currentFrequencyMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void pauseSimulation() {
        if (!running.get()) return;

        running.set(false);
        if (simulationTask != null) {
            simulationTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        LOGGER.info("Hardware scanner simulation paused.");
    }

    public synchronized void stopSimulation() {
        pauseSimulation();
        LOGGER.info("Hardware scanner simulation stopped.");
    }

    public void updateFrequency(int frequencyMs) {
        this.currentFrequencyMs = Math.max(50, frequencyMs);
        if (running.get()) {
            pauseSimulation();
            startSimulation(currentFrequencyMs);
        }
    }

    public void injectManualAnomaly(String anomalyType) {
        ScanEventGenerator.RawBarcode raw = generator.generateExplicitAnomaly(anomalyType);
        processScanAsync(raw.stationId, raw.payload);
    }

    private void tickScan() {
        try {
            ScanEventGenerator.RawBarcode raw = generator.generateScan(anomalyRate);
            processScanAsync(raw.stationId, raw.payload);
        } catch (Exception e) {
            LOGGER.error("Error during scanner simulation tick", e);
        }
    }

    private void processScanAsync(String stationId, String payload) {
        ScanEvent event = pipelineProcessor.processRawScan(stationId, payload);

        totalScansCounter.incrementAndGet();
        switch (event.getStatus()) {
            case PROCESSED -> processedCounter.incrementAndGet();
            case QUARANTINE -> quarantineCounter.incrementAndGet();
            case REJECTED -> rejectedCounter.incrementAndGet();
            case PENDING -> {}
        }

        // Notify UI listeners
        for (ScanEventListener listener : listeners) {
            try {
                listener.onScanProcessed(event);
            } catch (Exception e) {
                LOGGER.warn("Failed to notify scan event listener", e);
            }
        }
    }

    public void addScanEventListener(ScanEventListener listener) {
        listeners.add(listener);
    }

    public void removeScanEventListener(ScanEventListener listener) {
        listeners.remove(listener);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getTotalScans() {
        return totalScansCounter.get();
    }

    public long getProcessedCount() {
        return processedCounter.get();
    }

    public long getQuarantineCount() {
        return quarantineCounter.get();
    }

    public long getRejectedCount() {
        return rejectedCounter.get();
    }

    public double getQuarantineRatePct() {
        long total = totalScansCounter.get();
        if (total == 0) return 0.0;
        return (double) quarantineCounter.get() / total * 100.0;
    }

    public double getErrorRatePct() {
        long total = totalScansCounter.get();
        if (total == 0) return 0.0;
        return (double) (quarantineCounter.get() + rejectedCounter.get()) / total * 100.0;
    }

    public void resetCounters() {
        totalScansCounter.set(0);
        processedCounter.set(0);
        quarantineCounter.set(0);
        rejectedCounter.set(0);
    }

    public void setAnomalyRate(double rate) {
        this.anomalyRate = Math.max(0.0, Math.min(1.0, rate));
    }

    public double getAnomalyRate() {
        return anomalyRate;
    }

    public int getCurrentFrequencyMs() {
        return currentFrequencyMs;
    }
}

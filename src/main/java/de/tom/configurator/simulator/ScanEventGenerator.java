package de.tom.configurator.simulator;

import java.util.List;
import java.util.Random;

/**
 * Generator for industrial barcodes and test telemetry data.
 * Simulates high-speed conveyor belt scans with normal distributions and controlled anomalies.
 */
public class ScanEventGenerator {

    private final Random random = new Random();

    public static class RawBarcode {
        public final String stationId;
        public final String payload;

        public RawBarcode(String stationId, String payload) {
            this.stationId = stationId;
            this.payload = payload;
        }
    }

    private static final List<RawBarcode> VALID_TEMPLATES = List.of(
            // GS1 128 Palettenscans
            new RawBarcode("STATION_01_INBOUND", "010401234510001810LOT8819A310000450"),
            new RawBarcode("STATION_01_INBOUND", "010401234510001810LOT8820B310000455"),
            new RawBarcode("STATION_01_INBOUND", "010401234510002510KNORR001310003800"),
            new RawBarcode("STATION_01_INBOUND", "010401234510002510KNORR002310003780"),

            // Automotive Sub-Assemblies
            new RawBarcode("STATION_03_ASSEMBLY", "BC-KNORR901-2024"),
            new RawBarcode("STATION_03_ASSEMBLY", "BC-KNORR901-2025"),
            new RawBarcode("STATION_02_SORTING", "BC-KNORR901-2026"),

            // Express Dispatch Labels
            new RawBarcode("STATION_04_DISPATCH", "EXP-BOS-2041-W4200"),
            new RawBarcode("STATION_04_DISPATCH", "EXP-BOS-2041-W4180"),
            new RawBarcode("STATION_04_DISPATCH", "EXP-BOS-2041-W4220")
    );

    private static final List<RawBarcode> ANOMALY_TEMPLATES = List.of(
            // Weight Divergences (Trigger QUARANTINE: delta > 15%)
            new RawBarcode("STATION_01_INBOUND", "010401234510001810LOT8819A310000750"), // 750g vs 450g (+66%)
            new RawBarcode("STATION_01_INBOUND", "010401234510002510KNORR001310002200"), // 2200g vs 3800g (-42%)
            new RawBarcode("STATION_04_DISPATCH", "EXP-BOS-2041-W5900"),                  // 5900g vs 4200g (+40%)

            // Unknown SKUs (Trigger REJECTED)
            new RawBarcode("STATION_04_DISPATCH", "EXP-XYZ-9999-W4200"),
            new RawBarcode("STATION_03_ASSEMBLY", "BC-UNKN9999-1001"),

            // Syntax Corruptions (Trigger REJECTED)
            new RawBarcode("STATION_02_SORTING", "ERR_GARBAGE_PAYLOAD_%$#_88912"),
            new RawBarcode("STATION_01_INBOUND", "01040123451000-INVALID-LENGTH"),
            new RawBarcode("STATION_04_DISPATCH", "EXP-INCOMPLETE")
    );

    /**
     * Generates a scan payload based on anomaly probability.
     */
    public RawBarcode generateScan(double anomalyProbability) {
        if (random.nextDouble() < anomalyProbability) {
            return ANOMALY_TEMPLATES.get(random.nextInt(ANOMALY_TEMPLATES.size()));
        } else {
            return VALID_TEMPLATES.get(random.nextInt(VALID_TEMPLATES.size()));
        }
    }

    /**
     * Injects an explicit anomaly.
     */
    public RawBarcode generateExplicitAnomaly(String anomalyType) {
        if ("WEIGHT_DIVERGENCE".equalsIgnoreCase(anomalyType)) {
            return new RawBarcode("STATION_04_DISPATCH", "EXP-BOS-2041-W6500");
        } else if ("UNKNOWN_SKU".equalsIgnoreCase(anomalyType)) {
            return new RawBarcode("STATION_03_ASSEMBLY", "BC-GHOST000-9999");
        } else {
            return new RawBarcode("STATION_02_SORTING", "!!!CORRUPT_SENSOR_BUFFER_OVERFLOW!!!");
        }
    }
}

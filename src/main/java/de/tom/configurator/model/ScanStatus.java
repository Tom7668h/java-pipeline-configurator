package de.tom.configurator.model;

/**
 * Lifecycle status of a hardware scan event within the pipeline.
 */
public enum ScanStatus {
    PROCESSED("PROCESSED"),
    QUARANTINE("QUARANTINE"),
    REJECTED("REJECTED"),
    PENDING("PENDING");

    private final String label;

    ScanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

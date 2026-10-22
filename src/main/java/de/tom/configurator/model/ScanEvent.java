package de.tom.configurator.model;

import java.sql.Timestamp;

/**
 * Hardware Scan Event representing an incoming sensor read and its pipeline evaluation.
 */
public class ScanEvent {

    private Long logId;
    private String stationId;
    private String rawPayload;
    private String extractedSku;
    private String extractedLot;
    private Integer extractedWeightG;
    private ScanStatus status;
    private String statusReason;
    private long processingTimeMs;
    private Timestamp scannedAt;

    // Optional metadata enriched during processing
    private String matchedRuleName;
    private String targetAction;
    private Integer expectedWeightG;

    public ScanEvent() {
        this.status = ScanStatus.PENDING;
        this.scannedAt = new Timestamp(System.currentTimeMillis());
    }

    public ScanEvent(String stationId, String rawPayload) {
        this();
        this.stationId = stationId;
        this.rawPayload = rawPayload;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getExtractedSku() {
        return extractedSku;
    }

    public void setExtractedSku(String extractedSku) {
        this.extractedSku = extractedSku;
    }

    public String getExtractedLot() {
        return extractedLot;
    }

    public void setExtractedLot(String extractedLot) {
        this.extractedLot = extractedLot;
    }

    public Integer getExtractedWeightG() {
        return extractedWeightG;
    }

    public void setExtractedWeightG(Integer extractedWeightG) {
        this.extractedWeightG = extractedWeightG;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Timestamp getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Timestamp scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getMatchedRuleName() {
        return matchedRuleName;
    }

    public void setMatchedRuleName(String matchedRuleName) {
        this.matchedRuleName = matchedRuleName;
    }

    public String getTargetAction() {
        return targetAction;
    }

    public void setTargetAction(String targetAction) {
        this.targetAction = targetAction;
    }

    public Integer getExpectedWeightG() {
        return expectedWeightG;
    }

    public void setExpectedWeightG(Integer expectedWeightG) {
        this.expectedWeightG = expectedWeightG;
    }

    @Override
    public String toString() {
        return "ScanEvent{" +
                "logId=" + logId +
                ", stationId='" + stationId + '\'' +
                ", rawPayload='" + rawPayload + '\'' +
                ", extractedSku='" + extractedSku + '\'' +
                ", status=" + status +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}

package de.tom.configurator.model;

import java.sql.Timestamp;

/**
 * Domain entity representing a hardware barcode routing rule.
 */
public class RoutingRule {

    private Integer ruleId;
    private String name;
    private String regexPattern;
    private String stationId;
    private String targetAction;
    private int priority;
    private boolean active;
    private Timestamp createdAt;

    public RoutingRule() {
        this.active = true;
        this.priority = 10;
    }

    public RoutingRule(Integer ruleId, String name, String regexPattern, String stationId, String targetAction, int priority, boolean active) {
        this.ruleId = ruleId;
        this.name = name;
        this.regexPattern = regexPattern;
        this.stationId = stationId;
        this.targetAction = targetAction;
        this.priority = priority;
        this.active = active;
    }

    public Integer getRuleId() {
        return ruleId;
    }

    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getTargetAction() {
        return targetAction;
    }

    public void setTargetAction(String targetAction) {
        this.targetAction = targetAction;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RoutingRule{" +
                "ruleId=" + ruleId +
                ", name='" + name + '\'' +
                ", regexPattern='" + regexPattern + '\'' +
                ", stationId='" + stationId + '\'' +
                ", targetAction='" + targetAction + '\'' +
                ", priority=" + priority +
                ", active=" + active +
                '}';
    }
}

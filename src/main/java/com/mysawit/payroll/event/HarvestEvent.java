package com.mysawit.payroll.event;

import java.time.OffsetDateTime;

public class HarvestEvent extends PayrollEvent {
    private String harvestId;
    private String harvesterId;
    private String foremanId;
    private String plantationId;
    private Double weight;
    private String status;
    private OffsetDateTime occurredAt;

    public String getHarvestId() {
        return harvestId;
    }

    public void setHarvestId(String harvestId) {
        this.harvestId = harvestId;
    }

    public String getHarvesterId() {
        return harvesterId;
    }

    public void setHarvesterId(String harvesterId) {
        this.harvesterId = harvesterId;
    }

    public String getForemanId() {
        return foremanId;
    }

    public void setForemanId(String foremanId) {
        this.foremanId = foremanId;
    }

    public String getPlantationId() {
        return plantationId;
    }

    public void setPlantationId(String plantationId) {
        this.plantationId = plantationId;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}

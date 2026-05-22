package com.mysawit.payroll.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollEvent {
    private String eventId;

    @JsonAlias({"harvesterId"})
    private String employeeId;

    @JsonAlias({"employeeRole"})
    private String roleType;
    private double amount;

    @JsonAlias({"weight", "kg"})
    private Double kilograms;
    private Double totalKg;
    private Double recognizedKg;
    private String driverId;
    private String mandorId;

    @JsonAlias({"harvestId", "shipmentId"})
    private String sourceReference;
    private String description;
    private long timestamp;

    @JsonAlias({"occurredAt"})
    private OffsetDateTime occurredAt;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Double getKilograms() { return kilograms; }
    public void setKilograms(Double kilograms) { this.kilograms = kilograms; }
    public Double getTotalKg() { return totalKg; }
    public void setTotalKg(Double totalKg) { this.totalKg = totalKg; }
    public Double getRecognizedKg() { return recognizedKg; }
    public void setRecognizedKg(Double recognizedKg) { this.recognizedKg = recognizedKg; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getMandorId() { return mandorId; }
    public void setMandorId(String mandorId) { this.mandorId = mandorId; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}

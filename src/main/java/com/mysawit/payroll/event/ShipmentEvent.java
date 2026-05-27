package com.mysawit.payroll.event;

import java.time.OffsetDateTime;
import java.util.List;

public class ShipmentEvent extends PayrollEvent {
    private String shipmentId;
    private String employeeRole;
    private Double kg;
    private List<String> harvestIds;
    private OffsetDateTime occurredAt;

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(String employeeRole) {
        this.employeeRole = employeeRole;
    }

    public Double getKg() {
        return kg;
    }

    public void setKg(Double kg) {
        this.kg = kg;
    }

    public List<String> getHarvestIds() {
        return harvestIds;
    }

    public void setHarvestIds(List<String> harvestIds) {
        this.harvestIds = harvestIds;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}

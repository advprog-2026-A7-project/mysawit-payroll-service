package com.mysawit.payroll.event;

public class ShipmentEvent {
    private String eventId;
    private String employeeId;
    private double amount;
    private long timestamp;

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

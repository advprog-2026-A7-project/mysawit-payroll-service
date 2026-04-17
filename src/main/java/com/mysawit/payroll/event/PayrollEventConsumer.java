package com.mysawit.payroll.event;

public interface PayrollEventConsumer {
    void handleHarvestEvent(HarvestEvent event);
    void handleShipmentEvent(ShipmentEvent event);
}

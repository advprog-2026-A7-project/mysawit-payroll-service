package com.mysawit.payroll.event;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShipmentEventTest {

    @Test
    void defaultConstructorLeavesFieldsNull() {
        ShipmentEvent event = new ShipmentEvent();

        assertNull(event.getShipmentId());
        assertNull(event.getEmployeeRole());
        assertNull(event.getKg());
        assertNull(event.getHarvestIds());
        assertNull(event.getOccurredAt());
    }

    @Test
    void accessorsRoundTripValues() {
        ShipmentEvent event = new ShipmentEvent();
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-05-22T11:00:00+07:00");
        List<String> harvestIds = List.of("harvest-1", "harvest-2");

        event.setEventId("evt-1");
        event.setEmployeeId("supir-1");
        event.setAmount(75000.0);
        event.setShipmentId("shipment-1");
        event.setEmployeeRole("SUPIR");
        event.setKg(250.0);
        event.setHarvestIds(harvestIds);
        event.setOccurredAt(occurredAt);

        assertEquals("evt-1", event.getEventId());
        assertEquals("supir-1", event.getEmployeeId());
        assertEquals(75000.0, event.getAmount());
        assertEquals("shipment-1", event.getShipmentId());
        assertEquals("SUPIR", event.getEmployeeRole());
        assertEquals(250.0, event.getKg());
        assertEquals(harvestIds, event.getHarvestIds());
        assertEquals(2, event.getHarvestIds().size());
        assertEquals(occurredAt, event.getOccurredAt());
        assertNotNull(event.getOccurredAt());
    }
}

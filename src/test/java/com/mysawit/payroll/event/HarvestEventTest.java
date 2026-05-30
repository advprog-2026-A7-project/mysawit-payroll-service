package com.mysawit.payroll.event;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HarvestEventTest {

    @Test
    void defaultConstructorLeavesFieldsNull() {
        HarvestEvent event = new HarvestEvent();

        assertNull(event.getHarvestId());
        assertNull(event.getHarvesterId());
        assertNull(event.getForemanId());
        assertNull(event.getPlantationId());
        assertNull(event.getWeight());
        assertNull(event.getStatus());
        assertNull(event.getOccurredAt());
    }

    @Test
    void accessorsRoundTripValues() {
        HarvestEvent event = new HarvestEvent();
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-05-22T10:15:30+07:00");

        event.setEventId("evt-1");
        event.setEmployeeId("worker-1");
        event.setAmount(50000.0);
        event.setHarvestId("harvest-1");
        event.setHarvesterId("buruh-1");
        event.setForemanId("mandor-1");
        event.setPlantationId("plantation-1");
        event.setWeight(125.5);
        event.setStatus("APPROVED");
        event.setOccurredAt(occurredAt);

        assertEquals("evt-1", event.getEventId());
        assertEquals("worker-1", event.getEmployeeId());
        assertEquals(50000.0, event.getAmount());
        assertEquals("harvest-1", event.getHarvestId());
        assertEquals("buruh-1", event.getHarvesterId());
        assertEquals("mandor-1", event.getForemanId());
        assertEquals("plantation-1", event.getPlantationId());
        assertEquals(125.5, event.getWeight());
        assertEquals("APPROVED", event.getStatus());
        assertEquals(occurredAt, event.getOccurredAt());
        assertNotNull(event.getOccurredAt());
    }
}

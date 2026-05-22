package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PayrollBrokerEventTest {

    @Test
    void defaultConstructorAndSettersWork() {
        PayrollBrokerEvent event = new PayrollBrokerEvent();
        LocalDateTime processedAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 22, 10, 1);

        event.setId(1L);
        event.setEventKey("evt-1:BURUH:user-1");
        event.setEventId("evt-1");
        event.setEventType("HarvestEvent");
        event.setSourceType("HARVEST");
        event.setSourceReference("harvest-1");
        event.setUserId("user-1");
        event.setRoleType("BURUH");
        event.setKilograms(100.0);
        event.setAmount(31500.0);
        event.setPayrollId(2L);
        event.setProcessedAt(processedAt);
        event.setCreatedAt(createdAt);

        assertEquals(1L, event.getId());
        assertEquals("evt-1:BURUH:user-1", event.getEventKey());
        assertEquals("evt-1", event.getEventId());
        assertEquals("HarvestEvent", event.getEventType());
        assertEquals("HARVEST", event.getSourceType());
        assertEquals("harvest-1", event.getSourceReference());
        assertEquals("user-1", event.getUserId());
        assertEquals("BURUH", event.getRoleType());
        assertEquals(100.0, event.getKilograms());
        assertEquals(31500.0, event.getAmount());
        assertEquals(2L, event.getPayrollId());
        assertEquals(processedAt, event.getProcessedAt());
        assertEquals(createdAt, event.getCreatedAt());
    }

    @Test
    void lifecycleInitializesTimestamps() {
        PayrollBrokerEvent event = new PayrollBrokerEvent();

        event.onCreate();

        assertNotNull(event.getProcessedAt());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void lifecycleKeepsExistingProcessedAt() {
        PayrollBrokerEvent event = new PayrollBrokerEvent();
        LocalDateTime processedAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        event.setProcessedAt(processedAt);

        event.onCreate();

        assertEquals(processedAt, event.getProcessedAt());
        assertNotNull(event.getCreatedAt());
    }
}

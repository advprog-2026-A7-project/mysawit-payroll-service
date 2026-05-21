package com.mysawit.payroll.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayrollEventTest {

    @Test
    void payrollEventAccessorsWork() {
        PayrollEvent event = new PayrollEvent();

        event.setEventId("evt-001");
        event.setEmployeeId("worker-1");
        event.setRoleType("BURUH");
        event.setAmount(125000.0);
        event.setKilograms(100.0);
        event.setTotalKg(120.0);
        event.setRecognizedKg(110.0);
        event.setDriverId("driver-1");
        event.setMandorId("mandor-1");
        event.setSourceReference("harvest-1");
        event.setDescription("Approved harvest");
        event.setTimestamp(1779400000000L);

        assertEquals("evt-001", event.getEventId());
        assertEquals("worker-1", event.getEmployeeId());
        assertEquals("BURUH", event.getRoleType());
        assertEquals(125000.0, event.getAmount());
        assertEquals(100.0, event.getKilograms());
        assertEquals(120.0, event.getTotalKg());
        assertEquals(110.0, event.getRecognizedKg());
        assertEquals("driver-1", event.getDriverId());
        assertEquals("mandor-1", event.getMandorId());
        assertEquals("harvest-1", event.getSourceReference());
        assertEquals("Approved harvest", event.getDescription());
        assertEquals(1779400000000L, event.getTimestamp());
    }

    @Test
    void userRegisteredEventConstructorsAndAccessorsWork() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "u@example.com", "SUPIR", "udi");

        assertEquals("user-1", event.getUserId());
        assertEquals("u@example.com", event.getEmail());
        assertEquals("SUPIR", event.getRole());
        assertEquals("udi", event.getUsername());

        event.setUserId("user-2");
        event.setEmail("new@example.com");
        event.setRole("MANDOR");
        event.setUsername("new-name");

        assertEquals("user-2", event.getUserId());
        assertEquals("new@example.com", event.getEmail());
        assertEquals("MANDOR", event.getRole());
        assertEquals("new-name", event.getUsername());
    }
}

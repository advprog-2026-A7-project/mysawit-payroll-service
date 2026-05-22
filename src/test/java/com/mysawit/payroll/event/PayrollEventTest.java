package com.mysawit.payroll.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

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
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-05-22T10:15:30Z");
        event.setOccurredAt(occurredAt);

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
        assertEquals(occurredAt, event.getOccurredAt());
    }

    @Test
    void harvestPayloadAliasesPopulatePayrollFields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "eventId": "harvest-event-1",
                  "harvestId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "harvesterId": "11111111-1111-1111-1111-111111111111",
                  "foremanId": "22222222-2222-2222-2222-222222222222",
                  "plantationId": "00000000-0000-0000-0000-000000000001",
                  "weight": 125.5,
                  "status": "APPROVED",
                  "occurredAt": "2026-05-22T10:15:30Z"
                }
                """;

        HarvestEvent event = objectMapper.readValue(json, HarvestEvent.class);

        assertEquals("harvest-event-1", event.getEventId());
        assertEquals("11111111-1111-1111-1111-111111111111", event.getEmployeeId());
        assertEquals(125.5, event.getKilograms());
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", event.getSourceReference());
        assertEquals(OffsetDateTime.parse("2026-05-22T10:15:30Z"), event.getOccurredAt());
    }

    @Test
    void shipmentPayloadAliasesPopulatePayrollFields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "eventId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb:SUPIR",
                  "shipmentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                  "employeeId": "33333333-3333-3333-3333-333333333333",
                  "employeeRole": "SUPIR",
                  "kg": 250.0,
                  "harvestIds": ["aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"],
                  "occurredAt": "2026-05-22T11:00:00Z"
                }
                """;

        ShipmentEvent event = objectMapper.readValue(json, ShipmentEvent.class);

        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb:SUPIR", event.getEventId());
        assertEquals("33333333-3333-3333-3333-333333333333", event.getEmployeeId());
        assertEquals("SUPIR", event.getRoleType());
        assertEquals(250.0, event.getKilograms());
        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", event.getSourceReference());
        assertEquals(OffsetDateTime.parse("2026-05-22T11:00:00Z"), event.getOccurredAt());
    }

    @Test
    void userRegisteredEventConstructorsAndAccessorsWork() {
        assertNotNull(new UserRegisteredEvent());
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

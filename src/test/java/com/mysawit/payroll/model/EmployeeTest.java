package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    @Test
    void defaultConstructorAndSettersWork() {
        Employee employee = new Employee();
        LocalDateTime hireDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 1, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 1, 2, 0);

        assertEquals("ACTIVE", employee.getStatus());

        employee.setId(1L);
        employee.setName("Budi");
        employee.setEmployeeCode("EMP001");
        employee.setPosition("Harvester");
        employee.setPlantationId(2L);
        employee.setPhoneNumber("08123");
        employee.setAddress("Riau");
        employee.setHireDate(hireDate);
        employee.setBaseSalary(5000000.0);
        employee.setStatus("INACTIVE");
        employee.setCreatedAt(createdAt);
        employee.setUpdatedAt(updatedAt);

        assertEquals(1L, employee.getId());
        assertEquals("Budi", employee.getName());
        assertEquals("EMP001", employee.getEmployeeCode());
        assertEquals("Harvester", employee.getPosition());
        assertEquals(2L, employee.getPlantationId());
        assertEquals("08123", employee.getPhoneNumber());
        assertEquals("Riau", employee.getAddress());
        assertEquals(hireDate, employee.getHireDate());
        assertEquals(5000000.0, employee.getBaseSalary());
        assertEquals("INACTIVE", employee.getStatus());
        assertEquals(createdAt, employee.getCreatedAt());
        assertEquals(updatedAt, employee.getUpdatedAt());
    }

    @Test
    void customConstructorSetsFields() {
        Employee employee = new Employee("Budi", "EMP001", "Harvester", 5000000.0);

        assertEquals("Budi", employee.getName());
        assertEquals("EMP001", employee.getEmployeeCode());
        assertEquals("Harvester", employee.getPosition());
        assertEquals(5000000.0, employee.getBaseSalary());
        assertEquals("ACTIVE", employee.getStatus());
    }

    @Test
    void lifecycleHooksSetTimestamps() {
        Employee employee = new Employee();

        employee.onCreate();

        assertNotNull(employee.getCreatedAt());
        assertNotNull(employee.getUpdatedAt());

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        employee.setUpdatedAt(beforeUpdate.minusDays(1));

        employee.onUpdate();

        assertTrue(employee.getUpdatedAt().isAfter(beforeUpdate));
    }
}

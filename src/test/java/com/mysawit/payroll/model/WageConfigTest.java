package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WageConfigTest {

    @Test
    void defaultConstructorSetsNoFields() {
        WageConfig wageConfig = new WageConfig();
        assertNull(wageConfig.getId());
        assertNull(wageConfig.getRoleType());
        assertNull(wageConfig.getRatePerKg());
    }

    @Test
    void parameterizedConstructorSetsFields() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        WageConfig wageConfig = new WageConfig("BURUH", 350.0, date);

        assertEquals("BURUH", wageConfig.getRoleType());
        assertEquals(350.0, wageConfig.getRatePerKg());
        assertEquals(date, wageConfig.getEffectiveDate());
    }

    @Test
    void gettersAndSettersWorkCorrectly() {
        WageConfig wageConfig = new WageConfig();
        LocalDate date = LocalDate.of(2026, 3, 1);
        LocalDateTime now = LocalDateTime.now();

        wageConfig.setId(1L);
        wageConfig.setRoleType("SUPIR");
        wageConfig.setRatePerKg(250.0);
        wageConfig.setEffectiveDate(date);
        wageConfig.setDescription("Driver wage");
        wageConfig.setCreatedBy("admin");
        wageConfig.setCreatedAt(now);
        wageConfig.setUpdatedAt(now);

        assertEquals(1L, wageConfig.getId());
        assertEquals("SUPIR", wageConfig.getRoleType());
        assertEquals(250.0, wageConfig.getRatePerKg());
        assertEquals(date, wageConfig.getEffectiveDate());
        assertEquals("Driver wage", wageConfig.getDescription());
        assertEquals("admin", wageConfig.getCreatedBy());
        assertEquals(now, wageConfig.getCreatedAt());
        assertEquals(now, wageConfig.getUpdatedAt());
    }

    @Test
    void onCreateSetsTimestamps() throws Exception {
        WageConfig wageConfig = new WageConfig();
        var method = WageConfig.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(wageConfig);

        assertNotNull(wageConfig.getCreatedAt());
        assertNotNull(wageConfig.getUpdatedAt());
    }

    @Test
    void onUpdateSetsUpdatedAt() throws Exception {
        WageConfig wageConfig = new WageConfig();
        var method = WageConfig.class.getDeclaredMethod("onUpdate");
        method.setAccessible(true);
        method.invoke(wageConfig);

        assertNotNull(wageConfig.getUpdatedAt());
    }
}

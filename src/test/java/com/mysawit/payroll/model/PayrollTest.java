package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PayrollTest {

    @Test
    void defaultConstructorAndSettersWork() {
        Payroll payroll = new Payroll();
        LocalDateTime periodStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 1, 31, 23, 59);
        LocalDateTime paymentDate = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 1, 11, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 1, 12, 0);

        assertEquals(0.0, payroll.getBonusAmount());
        assertEquals(0.0, payroll.getDeductionAmount());

        payroll.setId(1L);
        payroll.setEmployeeId(2L);
        payroll.setPeriodStart(periodStart);
        payroll.setPeriodEnd(periodEnd);
        payroll.setBaseAmount(5000000.0);
        payroll.setBonusAmount(500000.0);
        payroll.setDeductionAmount(250000.0);
        payroll.setTotalAmount(5250000.0);
        payroll.setStatus("PENDING");
        payroll.setPaymentDate(paymentDate);
        payroll.setPaymentMethod("BANK_TRANSFER");
        payroll.setNotes("notes");
        payroll.setCreatedAt(createdAt);
        payroll.setUpdatedAt(updatedAt);

        assertEquals(1L, payroll.getId());
        assertEquals(2L, payroll.getEmployeeId());
        assertEquals(periodStart, payroll.getPeriodStart());
        assertEquals(periodEnd, payroll.getPeriodEnd());
        assertEquals(5000000.0, payroll.getBaseAmount());
        assertEquals(500000.0, payroll.getBonusAmount());
        assertEquals(250000.0, payroll.getDeductionAmount());
        assertEquals(5250000.0, payroll.getTotalAmount());
        assertEquals("PENDING", payroll.getStatus());
        assertEquals(paymentDate, payroll.getPaymentDate());
        assertEquals("BANK_TRANSFER", payroll.getPaymentMethod());
        assertEquals("notes", payroll.getNotes());
        assertEquals(createdAt, payroll.getCreatedAt());
        assertEquals(updatedAt, payroll.getUpdatedAt());
    }

    @Test
    void customConstructorCalculatesTotal() {
        Payroll payroll = new Payroll(
                2L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59),
                5000000.0,
                "PENDING"
        );

        assertEquals(5000000.0, payroll.getTotalAmount());
        assertEquals("PENDING", payroll.getStatus());
        assertEquals(0.0, payroll.getBonusAmount());
        assertEquals(0.0, payroll.getDeductionAmount());
    }

    @Test
    void lifecycleHooksSetTimestampsAndRecalculateTotal() {
        Payroll payroll = new Payroll();
        payroll.setBaseAmount(4000000.0);
        payroll.setBonusAmount(500000.0);
        payroll.setDeductionAmount(200000.0);

        payroll.onCreate();

        assertNotNull(payroll.getCreatedAt());
        assertNotNull(payroll.getUpdatedAt());
        assertEquals(4300000.0, payroll.getTotalAmount());

        payroll.setBaseAmount(4500000.0);
        payroll.setBonusAmount(600000.0);
        payroll.setDeductionAmount(100000.0);

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        payroll.setUpdatedAt(beforeUpdate.minusDays(1));

        payroll.onUpdate();

        assertEquals(5000000.0, payroll.getTotalAmount());
        assertTrue(payroll.getUpdatedAt().isAfter(beforeUpdate));
    }
}

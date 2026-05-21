package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTransactionTest {

    @Test
    void gettersAndSettersWork() {
        PaymentTransaction transaction = new PaymentTransaction();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 22, 10, 1);

        transaction.setId(1L);
        transaction.setTransactionId("sandbox-001");
        transaction.setUserId("admin");
        transaction.setGateway("XENDIT_SANDBOX");
        transaction.setStatus("PAID");
        transaction.setAmountSawitDollar(50.0);
        transaction.setAmountIdr(500000.0);
        transaction.setCheckoutUrl("sandbox://payment/sandbox-001");
        transaction.setCreatedAt(createdAt);
        transaction.setPaidAt(paidAt);

        assertEquals(1L, transaction.getId());
        assertEquals("sandbox-001", transaction.getTransactionId());
        assertEquals("admin", transaction.getUserId());
        assertEquals("XENDIT_SANDBOX", transaction.getGateway());
        assertEquals("PAID", transaction.getStatus());
        assertEquals(50.0, transaction.getAmountSawitDollar());
        assertEquals(500000.0, transaction.getAmountIdr());
        assertEquals("sandbox://payment/sandbox-001", transaction.getCheckoutUrl());
        assertEquals(createdAt, transaction.getCreatedAt());
        assertEquals(paidAt, transaction.getPaidAt());
    }

    @Test
    void onCreateSetsCreatedAt() {
        PaymentTransaction transaction = new PaymentTransaction();

        transaction.onCreate();

        assertNotNull(transaction.getCreatedAt());
    }
}

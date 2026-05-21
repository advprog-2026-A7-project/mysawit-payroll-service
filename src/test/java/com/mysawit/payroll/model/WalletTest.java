package com.mysawit.payroll.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void constructorCreatesZeroBalanceWalletForUser() {
        Wallet wallet = new Wallet("admin");

        assertEquals("admin", wallet.getUserId());
        assertEquals(0.0, wallet.getBalance());
    }

    @Test
    void gettersAndSettersWork() {
        Wallet wallet = new Wallet();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 22, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 22, 9, 0);

        wallet.setId(10L);
        wallet.setUserId("worker-1");
        wallet.setBalance(250.0);
        wallet.setCreatedAt(createdAt);
        wallet.setUpdatedAt(updatedAt);

        assertEquals(10L, wallet.getId());
        assertEquals("worker-1", wallet.getUserId());
        assertEquals(250.0, wallet.getBalance());
        assertEquals(createdAt, wallet.getCreatedAt());
        assertEquals(updatedAt, wallet.getUpdatedAt());
    }

    @Test
    void onCreateSetsTimestampsAndDefaultsNullBalance() {
        Wallet wallet = new Wallet("worker-2");
        wallet.setBalance(null);

        wallet.onCreate();

        assertEquals(0.0, wallet.getBalance());
        assertNotNull(wallet.getCreatedAt());
        assertNotNull(wallet.getUpdatedAt());
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Wallet wallet = new Wallet("worker-3");
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
        wallet.setUpdatedAt(oldUpdatedAt);

        wallet.onUpdate();

        assertTrue(wallet.getUpdatedAt().isAfter(oldUpdatedAt));
    }
}

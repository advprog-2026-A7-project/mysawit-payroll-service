package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    private WalletController walletController;

    @BeforeEach
    void setUp() {
        walletController = new WalletController(walletService);
    }

    @Test
    void getWalletReturnsWallet() {
        Wallet wallet = new Wallet("admin-1");
        when(walletService.getOrCreateWallet("admin-1")).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.getWallet("admin-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("admin-1", response.getBody().getUserId());
    }

    @Test
    void getTransactionsReturnsTransactions() {
        when(walletService.getTransactions("admin-1")).thenReturn(List.of(new PaymentTransaction()));

        ResponseEntity<List<PaymentTransaction>> response = walletController.getTransactions("admin-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createSandboxTopUpReturnsTransaction() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("tx-1");
        when(walletService.createSandboxTopUp("admin-1", 10.0, "MIDTRANS_SANDBOX")).thenReturn(transaction);

        ResponseEntity<?> response = walletController.createSandboxTopUp(
                "admin-1",
                Map.of("amountSawitDollar", 10.0, "gateway", "MIDTRANS_SANDBOX")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createSandboxTopUpReturns400OnValidationError() {
        when(walletService.createSandboxTopUp("admin-1", -1.0, null))
                .thenThrow(new IllegalArgumentException("Top-up amount must be greater than 0 SawitDollar"));

        ResponseEntity<?> response = walletController.createSandboxTopUp("admin-1", Map.of("amountSawitDollar", -1.0));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void settleSandboxReturnsTransaction() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("tx-1");
        when(walletService.settleSandboxTransaction("tx-1", "PAID")).thenReturn(transaction);

        ResponseEntity<?> response = walletController.settleSandbox("tx-1", Map.of("status", "PAID"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void settleSandboxReturns404WhenMissing() {
        when(walletService.settleSandboxTransaction("tx-1", "PAID")).thenThrow(new RuntimeException("missing"));

        ResponseEntity<?> response = walletController.settleSandbox("tx-1", Map.of("status", "PAID"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

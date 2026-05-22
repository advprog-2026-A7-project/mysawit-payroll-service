package com.mysawit.payroll.controller;

import com.mysawit.payroll.config.MidtransSandboxProperties;
import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private MidtransSandboxProperties midtransProperties;

    @InjectMocks
    private WalletController walletController;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet("admin");
        wallet.setBalance(100.0);
    }

    @Test
    void getWalletReturnsWallet() {
        when(walletService.getOrCreateWallet("admin")).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.getWallet("admin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(wallet, response.getBody());
    }

    @Test
    void getTransactionsReturnsTransactions() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("sandbox-1");
        when(walletService.getTransactionsForUser("admin")).thenReturn(List.of(transaction));

        ResponseEntity<List<PaymentTransaction>> response = walletController.getTransactions("admin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void topUpSandboxReturnsTransaction() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("sandbox-1");
        when(walletService.topUpSandbox("admin", 50.0, "MIDTRANS_SANDBOX")).thenReturn(transaction);

        ResponseEntity<?> response = walletController.topUpSandbox(
                "admin",
                Map.of("amountSawitDollar", "50", "gateway", "MIDTRANS_SANDBOX"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transaction, response.getBody());
    }

    @Test
    void topUpSandboxUsesDefaultsAndReturnsBadRequestForInvalidInput() {
        when(walletService.topUpSandbox("admin", 0.0, "MIDTRANS_SANDBOX"))
                .thenThrow(new IllegalArgumentException("Top-up amount must be greater than zero"));

        ResponseEntity<?> response = walletController.topUpSandbox("admin", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void midtransWebhookSettlesPaidTransactionWhenSignatureIsNotConfigured() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("mysawit-topup-1");
        transaction.setStatus("PAID");
        when(midtransProperties.getServerKey()).thenReturn("");
        when(walletService.settleTopUp("mysawit-topup-1", "settlement")).thenReturn(transaction);

        ResponseEntity<?> response = walletController.handleMidtransWebhook(
                Map.of("order_id", "mysawit-topup-1", "transaction_status", "settlement"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transaction, response.getBody());
    }

    @Test
    void midtransWebhookRejectsInvalidSignature() {
        when(midtransProperties.getServerKey()).thenReturn("server-key");

        ResponseEntity<?> response = walletController.handleMidtransWebhook(
                Map.of(
                        "order_id", "mysawit-topup-1",
                        "transaction_status", "settlement",
                        "status_code", "200",
                        "gross_amount", "100000.00",
                        "signature_key", "bad-signature"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(walletService, never()).settleTopUp(anyString(), anyString());
    }

    @Test
    void settleSandboxTransactionDefaultsToPaid() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("mysawit-topup-1");
        transaction.setStatus("PAID");
        when(walletService.settleTopUp("mysawit-topup-1", "PAID")).thenReturn(transaction);

        ResponseEntity<?> response = walletController.settleSandboxTransaction("mysawit-topup-1", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transaction, response.getBody());
    }
}

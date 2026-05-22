package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
import com.mysawit.payroll.service.payment.PaymentGatewayClient;
import com.mysawit.payroll.service.payment.PaymentGatewayInvoice;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @InjectMocks
    private WalletService walletService;

    private Wallet adminWallet;
    private Wallet workerWallet;

    @BeforeEach
    void setUp() {
        adminWallet = new Wallet("admin");
        adminWallet.setBalance(1000.0);
        workerWallet = new Wallet("worker-1");
        workerWallet.setBalance(10.0);
    }

    @Test
    void getOrCreateWalletReturnsExistingWallet() {
        when(walletRepository.findByUserId("worker-1")).thenReturn(Optional.of(workerWallet));

        Wallet result = walletService.getOrCreateWallet("worker-1");

        assertSame(workerWallet, result);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getOrCreateWalletCreatesDefaultZeroBalanceWallet() {
        when(walletRepository.findByUserId("worker-2")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.getOrCreateWallet("worker-2");

        assertEquals("worker-2", result.getUserId());
        assertEquals(0.0, result.getBalance());
    }

    @Test
    void topUpSandboxCreatesPendingMidtransTransactionWithoutCreditingBalance() {
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));
        when(paymentGatewayClient.createTopUpInvoice(anyString(), eq("admin"), eq(25.0), eq(250000.0)))
                .thenReturn(new PaymentGatewayInvoice("midtrans-token-1", "PENDING", "https://app.sandbox.midtrans.com/snap/v4/redirection/token-1"));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.topUpSandbox("admin", 25.0, "midtrans_sandbox");

        assertEquals(1000.0, adminWallet.getBalance());
        assertEquals("PENDING", result.getStatus());
        assertEquals("MIDTRANS_SANDBOX", result.getGateway());
        assertEquals(250000.0, result.getAmountIdr());
        assertEquals("midtrans-token-1", result.getGatewayTransactionId());
        assertEquals("https://app.sandbox.midtrans.com/snap/v4/redirection/token-1", result.getCheckoutUrl());
        assertTrue(result.getTransactionId().startsWith("mysawit-topup-"));
    }

    @Test
    void getTransactionsForUserReturnsTransactions() {
        PaymentTransaction transaction = new PaymentTransaction();
        when(paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc("admin")).thenReturn(List.of(transaction));

        List<PaymentTransaction> result = walletService.getTransactionsForUser("admin");

        assertEquals(1, result.size());
    }

    @Test
    void topUpSandboxDefaultsGatewayWhenMissing() {
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));
        when(paymentGatewayClient.createTopUpInvoice(anyString(), eq("admin"), eq(10.0), eq(100000.0)))
                .thenReturn(new PaymentGatewayInvoice("midtrans-token-2", "PENDING", "https://app.sandbox.midtrans.com/snap/v4/redirection/token-2"));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.topUpSandbox("admin", 10.0, " ");

        assertEquals("MIDTRANS_SANDBOX", result.getGateway());
    }

    @Test
    void settleTopUpCreditsWalletWhenPaid() {
        PaymentTransaction transaction = pendingTransaction("admin", 25.0);
        when(paymentTransactionRepository.findByTransactionId("mysawit-topup-1")).thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.settleTopUp("mysawit-topup-1", "settlement");

        assertEquals("PAID", result.getStatus());
        assertEquals(1025.0, adminWallet.getBalance());
        assertNotNull(result.getPaidAt());
    }

    @Test
    void settleTopUpDoesNotCreditTwiceWhenAlreadyPaid() {
        PaymentTransaction transaction = pendingTransaction("admin", 25.0);
        transaction.setStatus("PAID");
        when(paymentTransactionRepository.findByTransactionId("mysawit-topup-1")).thenReturn(Optional.of(transaction));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.settleTopUp("mysawit-topup-1", "SETTLED");

        assertEquals("PAID", result.getStatus());
        assertEquals(1000.0, adminWallet.getBalance());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void settleTopUpStoresTerminalFailureStatuses() {
        PaymentTransaction transaction = pendingTransaction("admin", 25.0);
        when(paymentTransactionRepository.findByTransactionId("mysawit-topup-1")).thenReturn(Optional.of(transaction));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.settleTopUp("mysawit-topup-1", "EXPIRED");

        assertEquals("EXPIRED", result.getStatus());
        assertEquals(1000.0, adminWallet.getBalance());
    }

    @Test
    void transferMovesBalanceBetweenWallets() {
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));
        when(walletRepository.findByUserId("worker-1")).thenReturn(Optional.of(workerWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        double transferred = walletService.transfer("admin", "worker-1", 250.0);

        assertEquals(250.0, transferred);
        assertEquals(750.0, adminWallet.getBalance());
        assertEquals(260.0, workerWallet.getBalance());
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(2)).save(captor.capture());
    }

    @Test
    void transferRejectsInsufficientBalance() {
        when(walletRepository.findByUserId("worker-1")).thenReturn(Optional.of(workerWallet));
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));

        assertThrows(IllegalStateException.class, () -> walletService.transfer("worker-1", "admin", 100.0));
    }

    @Test
    void topUpSandboxRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> walletService.topUpSandbox("admin", 0.0, "sandbox"));
    }

    @Test
    void transferRejectsNonPositiveAmountAndInvalidUserIds() {
        assertThrows(IllegalArgumentException.class, () -> walletService.transfer("admin", "worker-1", 0.0));
        assertThrows(IllegalArgumentException.class, () -> walletService.getOrCreateWallet(" "));
        assertThrows(IllegalArgumentException.class, () -> walletService.getTransactionsForUser(null));
    }

    private PaymentTransaction pendingTransaction(String userId, double amountSawitDollar) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("mysawit-topup-1");
        transaction.setUserId(userId);
        transaction.setGateway("MIDTRANS_SANDBOX");
        transaction.setStatus("PENDING");
        transaction.setAmountSawitDollar(amountSawitDollar);
        transaction.setAmountIdr(amountSawitDollar * WalletService.IDR_PER_SAWIT_DOLLAR);
        transaction.setCheckoutUrl("https://app.sandbox.midtrans.com/snap/v4/redirection/token-1");
        return transaction;
    }
}

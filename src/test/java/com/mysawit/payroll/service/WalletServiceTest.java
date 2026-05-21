package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
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
    void topUpSandboxAddsBalanceAndCreatesPaidTransaction() {
        when(walletRepository.findByUserId("admin")).thenReturn(Optional.of(adminWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = walletService.topUpSandbox("admin", 25.0, "xendit_sandbox");

        assertEquals(1025.0, adminWallet.getBalance());
        assertEquals("PAID", result.getStatus());
        assertEquals("XENDIT_SANDBOX", result.getGateway());
        assertEquals(250000.0, result.getAmountIdr());
        assertTrue(result.getCheckoutUrl().startsWith("sandbox://payment/"));
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
}

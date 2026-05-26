package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, transactionRepository, 10000.0);
    }

    @Test
    void getOrCreateWalletCreatesMissingWallet() {
        when(walletRepository.findByUserId("admin-1")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = walletService.getOrCreateWallet("admin-1");

        assertEquals("admin-1", wallet.getUserId());
        assertEquals(0.0, wallet.getBalance());
    }

    @Test
    void createSandboxTopUpCreatesPendingTransaction() {
        when(walletRepository.findByUserId("admin-1")).thenReturn(Optional.of(new Wallet("admin-1")));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction transaction = walletService.createSandboxTopUp("admin-1", 25.0, "midtrans_sandbox");

        assertEquals("admin-1", transaction.getUserId());
        assertEquals("MIDTRANS_SANDBOX", transaction.getGateway());
        assertEquals("PENDING", transaction.getStatus());
        assertEquals(25.0, transaction.getAmountSawitDollar());
        assertEquals(250000.0, transaction.getAmountIdr());
        assertNotNull(transaction.getCheckoutUrl());
    }

    @Test
    void settleSandboxPaidCreditsWalletOnce() {
        Wallet wallet = new Wallet("admin-1");
        wallet.setBalance(5.0);
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("tx-1");
        transaction.setUserId("admin-1");
        transaction.setStatus("PENDING");
        transaction.setAmountSawitDollar(10.0);
        transaction.setAmountIdr(100000.0);

        when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserId("admin-1")).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction settled = walletService.settleSandboxTransaction("tx-1", "PAID");

        assertEquals("PAID", settled.getStatus());
        assertEquals(15.0, wallet.getBalance());
        assertNotNull(settled.getPaidAt());
        verify(walletRepository).save(wallet);
    }

    @Test
    void transferPayrollDebitsAdminAndCreditsRecipient() {
        Wallet admin = new Wallet("admin-1");
        admin.setBalance(100.0);
        Wallet worker = new Wallet("worker-1");
        worker.setBalance(2.0);
        when(walletRepository.findByUserId("admin-1")).thenReturn(Optional.of(admin));
        when(walletRepository.findByUserId("worker-1")).thenReturn(Optional.of(worker));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.transferPayroll("admin-1", "worker-1", 300000.0, "payroll-1");

        assertEquals(70.0, admin.getBalance());
        assertEquals(32.0, worker.getBalance());
        verify(transactionRepository, times(2)).save(any(PaymentTransaction.class));
    }

    @Test
    void transferPayrollThrowsWhenAdminBalanceInsufficient() {
        Wallet admin = new Wallet("admin-1");
        admin.setBalance(1.0);
        when(walletRepository.findByUserId("admin-1")).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class,
                () -> walletService.transferPayroll("admin-1", "worker-1", 300000.0, "payroll-1"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getTransactionsDelegatesToRepository() {
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc("admin-1")).thenReturn(List.of(new PaymentTransaction()));

        assertEquals(1, walletService.getTransactions("admin-1").size());
    }
}

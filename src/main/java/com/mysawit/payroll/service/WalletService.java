package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WalletService {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PENDING = "PENDING";

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final double sawitDollarRateIdr;

    public WalletService(
            WalletRepository walletRepository,
            PaymentTransactionRepository transactionRepository,
            @Value("${wallet.sawit-dollar-rate-idr:10000}") double sawitDollarRateIdr
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.sawitDollarRateIdr = sawitDollarRateIdr;
    }

    @Transactional
    public Wallet getOrCreateWallet(String userId) {
        validateUserId(userId);
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    public List<PaymentTransaction> getTransactions(String userId) {
        validateUserId(userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PaymentTransaction createSandboxTopUp(String userId, Double amountSawitDollar, String gateway) {
        validateUserId(userId);
        if (amountSawitDollar == null || amountSawitDollar <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than 0 SawitDollar");
        }

        getOrCreateWallet(userId);

        String transactionId = "sandbox-" + UUID.randomUUID();
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setUserId(userId);
        transaction.setGateway(normalizeGateway(gateway));
        transaction.setStatus(STATUS_PENDING);
        transaction.setAmountSawitDollar(amountSawitDollar);
        transaction.setAmountIdr(toIdr(amountSawitDollar));
        transaction.setCheckoutUrl("sandbox://payments/" + transactionId);
        transaction.setGatewayTransactionId(transactionId);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransaction settleSandboxTransaction(String transactionId, String status) {
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        String normalizedStatus = normalizeStatus(status);
        if (STATUS_PAID.equals(transaction.getStatus())) {
            return transaction;
        }

        transaction.setStatus(normalizedStatus);
        if (STATUS_PAID.equals(normalizedStatus)) {
            Wallet wallet = getOrCreateWallet(transaction.getUserId());
            wallet.setBalance(wallet.getBalance() + transaction.getAmountSawitDollar());
            walletRepository.save(wallet);
            transaction.setPaidAt(LocalDateTime.now());
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void transferPayroll(String adminId, String recipientUserId, Double amountIdr, String reference) {
        validateUserId(adminId);
        validateUserId(recipientUserId);
        if (amountIdr == null || amountIdr <= 0) {
            throw new IllegalArgumentException("Payroll amount must be greater than 0");
        }

        double sawitDollarAmount = toSawitDollar(amountIdr);
        Wallet adminWallet = getOrCreateWallet(adminId);
        if (adminWallet.getBalance() < sawitDollarAmount) {
            throw new IllegalStateException("Insufficient admin wallet balance");
        }

        Wallet recipientWallet = getOrCreateWallet(recipientUserId);
        adminWallet.setBalance(adminWallet.getBalance() - sawitDollarAmount);
        recipientWallet.setBalance(recipientWallet.getBalance() + sawitDollarAmount);
        walletRepository.save(adminWallet);
        walletRepository.save(recipientWallet);

        recordInternalTransfer(adminId, -sawitDollarAmount, -amountIdr, reference + ":debit");
        recordInternalTransfer(recipientUserId, sawitDollarAmount, amountIdr, reference + ":credit");
    }

    public double toSawitDollar(Double amountIdr) {
        return amountIdr / sawitDollarRateIdr;
    }

    private double toIdr(Double amountSawitDollar) {
        return amountSawitDollar * sawitDollarRateIdr;
    }

    private void recordInternalTransfer(String userId, double amountSawitDollar, double amountIdr, String reference) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(reference);
        transaction.setUserId(userId);
        transaction.setGateway("INTERNAL_WALLET");
        transaction.setStatus(STATUS_PAID);
        transaction.setAmountSawitDollar(amountSawitDollar);
        transaction.setAmountIdr(amountIdr);
        transaction.setGatewayTransactionId(reference);
        transaction.setPaidAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private String normalizeGateway(String gateway) {
        if (gateway == null || gateway.isBlank()) {
            return "MIDTRANS_SANDBOX";
        }
        return gateway.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PAID;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_PAID.equals(normalized) && !"FAILED".equals(normalized) && !"EXPIRED".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported sandbox settlement status: " + status);
        }
        return normalized;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}

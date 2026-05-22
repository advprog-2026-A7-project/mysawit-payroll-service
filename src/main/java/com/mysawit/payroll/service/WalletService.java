package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    public static final double IDR_PER_SAWIT_DOLLAR = 10000.0;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public Wallet getOrCreateWallet(String userId) {
        validateUserId(userId);
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    public List<PaymentTransaction> getTransactionsForUser(String userId) {
        validateUserId(userId);
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PaymentTransaction topUpSandbox(String userId, Double amountSawitDollar, String gateway) {
        validateUserId(userId);
        if (amountSawitDollar == null || amountSawitDollar <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amountSawitDollar);
        walletRepository.save(wallet);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("sandbox-" + UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setGateway(normalizeGateway(gateway));
        transaction.setStatus("PAID");
        transaction.setAmountSawitDollar(amountSawitDollar);
        transaction.setAmountIdr(amountSawitDollar * IDR_PER_SAWIT_DOLLAR);
        transaction.setCheckoutUrl("sandbox://payment/" + transaction.getTransactionId());
        transaction.setPaidAt(LocalDateTime.now());
        return paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public double transfer(String fromUserId, String toUserId, Double amount) {
        validateUserId(fromUserId);
        validateUserId(toUserId);
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        Wallet fromWallet = getOrCreateWallet(fromUserId);
        Wallet toWallet = getOrCreateWallet(toUserId);
        if (fromWallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient wallet balance for " + fromUserId);
        }

        fromWallet.setBalance(fromWallet.getBalance() - amount);
        toWallet.setBalance(toWallet.getBalance() + amount);
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        return amount;
    }

    private String normalizeGateway(String gateway) {
        if (gateway == null || gateway.isBlank()) {
            return "SANDBOX";
        }
        return gateway.toUpperCase();
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}

package com.mysawit.payroll.service;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.WalletRepository;
import com.mysawit.payroll.service.payment.PaymentGatewayClient;
import com.mysawit.payroll.service.payment.PaymentGatewayInvoice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    public static final double IDR_PER_SAWIT_DOLLAR = 10000.0;

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    public WalletService(
            WalletRepository walletRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGatewayClient paymentGatewayClient) {
        this.walletRepository = walletRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGatewayClient = paymentGatewayClient;
    }

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

        getOrCreateWallet(userId);
        double amountIdr = amountSawitDollar * IDR_PER_SAWIT_DOLLAR;
        String externalId = "mysawit-topup-" + UUID.randomUUID();
        PaymentGatewayInvoice invoice = paymentGatewayClient.createTopUpInvoice(
                externalId,
                userId,
                amountSawitDollar,
                amountIdr);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(externalId);
        transaction.setUserId(userId);
        transaction.setGateway(normalizeGateway(gateway));
        transaction.setStatus(normalizePaymentStatus(invoice.status()));
        transaction.setAmountSawitDollar(amountSawitDollar);
        transaction.setAmountIdr(amountIdr);
        transaction.setCheckoutUrl(invoice.checkoutUrl());
        transaction.setGatewayTransactionId(invoice.gatewayTransactionId());
        return paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransaction settleTopUp(String transactionId, String status) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found: " + transactionId));

        String normalizedStatus = normalizePaymentStatus(status);
        if ("PAID".equals(normalizedStatus)) {
            if (!"PAID".equals(transaction.getStatus())) {
                Wallet wallet = getOrCreateWallet(transaction.getUserId());
                wallet.setBalance(wallet.getBalance() + transaction.getAmountSawitDollar());
                walletRepository.save(wallet);
                transaction.setPaidAt(LocalDateTime.now());
            }
        }
        transaction.setStatus(normalizedStatus);
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
            return "MIDTRANS_SANDBOX";
        }
        return gateway.toUpperCase();
    }

    private String normalizePaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        String upper = status.toUpperCase();
        if ("SETTLEMENT".equals(upper) || "CAPTURE".equals(upper)
                || "SETTLED".equals(upper) || "SUCCEEDED".equals(upper) || "SUCCESS".equals(upper)) {
            return "PAID";
        }
        if ("PENDING".equals(upper)) {
            return "PENDING";
        }
        if ("EXPIRE".equals(upper) || "CANCEL".equals(upper)) {
            return "EXPIRED";
        }
        if ("DENY".equals(upper) || "FAILURE".equals(upper)) {
            return "FAILED";
        }
        if (!List.of("PENDING", "PAID", "EXPIRED", "FAILED").contains(upper)) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
        return upper;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}

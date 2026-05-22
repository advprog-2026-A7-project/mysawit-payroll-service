package com.mysawit.payroll.controller;

import com.mysawit.payroll.config.MidtransSandboxProperties;
import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.service.WalletService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class WalletController {

    private final WalletService walletService;
    private final MidtransSandboxProperties midtransProperties;

    public WalletController(WalletService walletService, MidtransSandboxProperties midtransProperties) {
        this.walletService = walletService;
        this.midtransProperties = midtransProperties;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getTransactionsForUser(userId));
    }

    @PostMapping("/{userId}/top-up/sandbox")
    public ResponseEntity<?> topUpSandbox(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            Map<String, String> body = request != null ? request : Map.of();
            Double amount = Double.valueOf(body.getOrDefault("amountSawitDollar", "0"));
            String gateway = body.getOrDefault("gateway", "MIDTRANS_SANDBOX");
            return ResponseEntity.ok(walletService.topUpSandbox(userId, amount, gateway));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/midtrans/webhook")
    public ResponseEntity<?> handleMidtransWebhook(@RequestBody Map<String, Object> payload) {
        try {
            validateMidtransSignature(payload);
            String transactionId = stringValue(payload.get("order_id"));
            String status = stringValue(payload.get("transaction_status"));
            PaymentTransaction transaction = walletService.settleTopUp(transactionId, status);
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transactions/{transactionId}/settle-sandbox")
    public ResponseEntity<?> settleSandboxTransaction(
            @PathVariable String transactionId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String status = request == null ? "PAID" : request.getOrDefault("status", "PAID");
            return ResponseEntity.ok(walletService.settleTopUp(transactionId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private void validateMidtransSignature(Map<String, Object> payload) {
        String serverKey = midtransProperties.getServerKey();
        if (serverKey == null || serverKey.isBlank()) {
            return;
        }
        String orderId = stringValue(payload.get("order_id"));
        String statusCode = stringValue(payload.get("status_code"));
        String grossAmount = stringValue(payload.get("gross_amount"));
        String signatureKey = stringValue(payload.get("signature_key"));
        if (orderId == null || statusCode == null || grossAmount == null || signatureKey == null) {
            throw new IllegalArgumentException("Midtrans signature fields are required");
        }
        String expectedSignature = sha512(orderId + statusCode + grossAmount + serverKey);
        if (!expectedSignature.equalsIgnoreCase(signatureKey)) {
            throw new IllegalArgumentException("Invalid Midtrans signature");
        }
    }

    private String sha512(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is not available", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}

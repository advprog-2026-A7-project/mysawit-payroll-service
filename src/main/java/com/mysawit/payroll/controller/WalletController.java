package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }

    @PostMapping("/{userId}/top-up/sandbox")
    public ResponseEntity<?> createSandboxTopUp(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Double amountSawitDollar = numberValue(request.get("amountSawitDollar"));
            String gateway = stringValue(request.get("gateway"));
            return ResponseEntity.ok(walletService.createSandboxTopUp(userId, amountSawitDollar, gateway));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transactions/{transactionId}/settle-sandbox")
    public ResponseEntity<?> settleSandbox(
            @PathVariable String transactionId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        try {
            String status = request == null ? "PAID" : request.getOrDefault("status", "PAID");
            return ResponseEntity.ok(walletService.settleSandboxTransaction(transactionId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Double.parseDouble(stringValue);
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

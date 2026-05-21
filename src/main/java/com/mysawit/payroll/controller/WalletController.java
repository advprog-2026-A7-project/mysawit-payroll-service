package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.PaymentTransaction;
import com.mysawit.payroll.model.Wallet;
import com.mysawit.payroll.service.WalletService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private WalletService walletService;

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
            String gateway = body.getOrDefault("gateway", "SANDBOX");
            return ResponseEntity.ok(walletService.topUpSandbox(userId, amount, gateway));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

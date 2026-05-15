package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payrolls")
@CrossOrigin(origins = {"${cors.allowed-origins}"})
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @GetMapping
    public ResponseEntity<List<Payroll>> getAllPayrolls() {
        return ResponseEntity.ok(payrollService.getAllPayrolls());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payroll> getPayrollById(@PathVariable Long id) {
        return payrollService.getPayrollById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payroll>> getPayrollsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(payrollService.getPayrollsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payroll>> getPayrollsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(payrollService.getPayrollsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Payroll> createPayroll(@RequestBody Payroll payroll) {
        Payroll created = payrollService.createPayroll(payroll);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payroll> updatePayroll(@PathVariable Long id, @RequestBody Payroll payroll) {
        try {
            Payroll updated = payrollService.updatePayroll(id, payroll);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Payroll> approvePayroll(@PathVariable Long id) {
        try {
            Payroll approved = payrollService.approvePayroll(id);
            return ResponseEntity.ok(approved);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<?> acceptPayroll(@PathVariable Long id) {
        try {
            Payroll accepted = payrollService.acceptPayroll(id);
            return ResponseEntity.ok(accepted);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectPayroll(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : null;
            Payroll rejected = payrollService.rejectPayroll(id, reason);
            return ResponseEntity.ok(rejected);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<Payroll> markAsPaid(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String paymentMethod = request.getOrDefault("paymentMethod", "BANK_TRANSFER");
            Payroll paid = payrollService.markAsPaid(id, paymentMethod);
            return ResponseEntity.ok(paid);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayroll(@PathVariable Long id) {
        try {
            payrollService.deletePayroll(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payrolls")
@CrossOrigin(origins = {"${cors.allowed-origins}"})
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @GetMapping
    public ResponseEntity<List<Payroll>> getAllPayrolls(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(payrollService.searchPayrolls(userId, status, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payroll> getPayrollById(@PathVariable Long id) {
        return payrollService.getPayrollById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Payroll>> getPayrollsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId));
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
    public ResponseEntity<?> approvePayroll(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request
    ) {
        try {
            String adminId = request == null ? null : request.get("adminId");
            Payroll approved = payrollService.approvePayroll(id, adminId);
            return ResponseEntity.ok(approved);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
        } catch (IllegalStateException | IllegalArgumentException e) {
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

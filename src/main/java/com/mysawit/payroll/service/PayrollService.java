package com.mysawit.payroll.service;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.client.IdentityClient;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired(required = false)
    private IdentityClient identityClient;

    @Value("${identity.service.token:}")
    private String identityServiceToken;

    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    public Optional<Payroll> getPayrollById(Long id) {
        return payrollRepository.findById(id);
    }

    public List<Payroll> getPayrollsByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    public List<Payroll> getPayrollsByStatus(String status) {
        return payrollRepository.findByStatus(status);
    }

    public Payroll createPayroll(Payroll payroll) {
        return payrollRepository.save(payroll);
    }

    public Payroll updatePayroll(Long id, Payroll payrollDetails) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));

        payroll.setBaseAmount(payrollDetails.getBaseAmount());
        payroll.setBonusAmount(payrollDetails.getBonusAmount());
        payroll.setDeductionAmount(payrollDetails.getDeductionAmount());
        payroll.setStatus(payrollDetails.getStatus());
        payroll.setPaymentDate(payrollDetails.getPaymentDate());
        payroll.setPaymentMethod(payrollDetails.getPaymentMethod());
        payroll.setNotes(payrollDetails.getNotes());

        return payrollRepository.save(payroll);
    }

    public Payroll approvePayroll(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
        payroll.setStatus("APPROVED");
        return payrollRepository.save(payroll);
    }

    /** Accept a payroll (alias for ACCEPTED status, semantically same as approve). */
    public Payroll acceptPayroll(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
        if (!"PENDING".equals(payroll.getStatus())) {
            throw new IllegalStateException("Only PENDING payrolls can be accepted. Current status: " + payroll.getStatus());
        }
        payroll.setStatus("ACCEPTED");
        return payrollRepository.save(payroll);
    }

    /** Reject a payroll with an optional reason stored in notes. */
    public Payroll rejectPayroll(Long id, String reason) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
        if (!"PENDING".equals(payroll.getStatus())) {
            throw new IllegalStateException("Only PENDING payrolls can be rejected. Current status: " + payroll.getStatus());
        }
        payroll.setStatus("REJECTED");
        if (reason != null && !reason.isBlank()) {
            payroll.setNotes(reason);
        }
        return payrollRepository.save(payroll);
    }

    public Payroll markAsPaid(Long id, String paymentMethod) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
        payroll.setStatus("PAID");
        payroll.setPaymentDate(LocalDateTime.now());
        payroll.setPaymentMethod(paymentMethod);
        return payrollRepository.save(payroll);
    }

    public void deletePayroll(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
        payrollRepository.delete(payroll);
    }

    // === Event-driven Payroll Generation ===

    public void processHarvestPayroll(HarvestEvent event) {
        processPayrollEvent(event.getEventId(), event.getEmployeeId(), event.getAmount(), "HarvestEvent");
    }


    public void processShipmentPayroll(ShipmentEvent event) {
        processPayrollEvent(event.getEventId(), event.getEmployeeId(), event.getAmount(), "ShipmentEvent");
    }

    // --- Refactored common logic for event payroll ---
    private void processPayrollEvent(String eventId, String employeeId, double amount, String eventType) {
        if (payrollRepository.findByEventId(eventId) != null) {
            return;
        }
        Payroll payroll = new Payroll();
        payroll.setEmployeeId(Long.valueOf(employeeId));
        payroll.setBaseAmount(amount);
        payroll.setBonusAmount(0.0);
        payroll.setDeductionAmount(0.0);
        payroll.setStatus("PENDING");
        payroll.setPeriodStart(java.time.LocalDateTime.now());
        payroll.setPeriodEnd(java.time.LocalDateTime.now());
        payroll.setNotes("Generated from " + eventType + ": " + eventId);
        payroll.setEventId(eventId);

        // Fetch user detail dari identity service
        if (identityClient != null && identityServiceToken != null && !identityServiceToken.isBlank()) {
            try {
                var userDetail = identityClient.getUserById(Long.valueOf(employeeId), "Bearer " + identityServiceToken);
                if (userDetail != null && userDetail.get("username") != null) {
                    payroll.setNotes(payroll.getNotes() + ", user: " + userDetail.get("username"));
                }
            } catch (Exception e) {
                payroll.setNotes(payroll.getNotes() + ", user fetch failed");
            }
        }

        payrollRepository.save(payroll);
    }
}

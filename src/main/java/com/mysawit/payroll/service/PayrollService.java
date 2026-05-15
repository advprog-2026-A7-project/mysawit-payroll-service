package com.mysawit.payroll.service;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.UserReplicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private UserReplicaRepository userReplicaRepository;

    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    public Optional<Payroll> getPayrollById(Long id) {
        return payrollRepository.findById(id);
    }

    public List<Payroll> getPayrollsByUser(String userId) {
        return payrollRepository.findByUserId(userId);
    }

    public List<Payroll> getPayrollsByStatus(String status) {
        return payrollRepository.findByStatus(status);
    }

    public Payroll createPayroll(Payroll payroll) {
        return payrollRepository.save(payroll);
    }

    public Payroll updatePayroll(Long id, Payroll payrollDetails) {
        Payroll payroll = getPayrollOrThrow(id);
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
        Payroll payroll = getPayrollOrThrow(id);
        payroll.setStatus("APPROVED");
        return payrollRepository.save(payroll);
    }

    public Payroll acceptPayroll(Long id) {
        Payroll payroll = getPayrollOrThrow(id);
        if (!"PENDING".equals(payroll.getStatus())) {
            throw new IllegalStateException("Only PENDING payrolls can be accepted. Current status: " + payroll.getStatus());
        }
        payroll.setStatus("ACCEPTED");
        return payrollRepository.save(payroll);
    }

    public Payroll rejectPayroll(Long id, String reason) {
        Payroll payroll = getPayrollOrThrow(id);
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
        Payroll payroll = getPayrollOrThrow(id);
        payroll.setStatus("PAID");
        payroll.setPaymentDate(LocalDateTime.now());
        payroll.setPaymentMethod(paymentMethod);
        return payrollRepository.save(payroll);
    }

    public void deletePayroll(Long id) {
        payrollRepository.delete(getPayrollOrThrow(id));
    }

    // === Event-driven Payroll Generation ===

    public void processHarvestPayroll(HarvestEvent event) {
        processPayrollEvent(event.getEventId(), event.getEmployeeId(), event.getAmount(), "HarvestEvent");
    }

    public void processShipmentPayroll(ShipmentEvent event) {
        processPayrollEvent(event.getEventId(), event.getEmployeeId(), event.getAmount(), "ShipmentEvent");
    }

    private void processPayrollEvent(String eventId, String userId, double amount, String eventType) {
        if (payrollRepository.findByEventId(eventId) != null) {
            return;
        }
        Payroll payroll = new Payroll();
        payroll.setUserId(userId);
        payroll.setBaseAmount(amount);
        payroll.setBonusAmount(0.0);
        payroll.setDeductionAmount(0.0);
        payroll.setStatus("PENDING");
        payroll.setPeriodStart(LocalDateTime.now());
        payroll.setPeriodEnd(LocalDateTime.now());
        payroll.setNotes(buildNotes(eventType, eventId, userId));
        payroll.setEventId(eventId);

        payrollRepository.save(payroll);
    }

    private String buildNotes(String eventType, String eventId, String userId) {
        String base = "Generated from " + eventType + ": " + eventId;
        return userReplicaRepository.findById(userId)
                .map(user -> base + ", user: " + user.getName())
                .orElse(base);
    }

    private Payroll getPayrollOrThrow(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
    }
}

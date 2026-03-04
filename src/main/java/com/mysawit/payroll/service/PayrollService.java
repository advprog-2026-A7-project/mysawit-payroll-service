package com.mysawit.payroll.service;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

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
}

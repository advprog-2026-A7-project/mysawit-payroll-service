package com.mysawit.payroll.service;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.UserReplicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private UserReplicaRepository userReplicaRepository;

    @Autowired
    private WageConfigService wageConfigService;

    @Autowired
    private WalletService walletService;

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
        return payrollRepository.findByStatus(normalizeStatus(status));
    }

    public List<Payroll> searchPayrolls(String userId, String status, LocalDateTime start, LocalDateTime end) {
        String normalizedStatus = status == null || status.isBlank() ? null : normalizeStatus(status);
        boolean hasUser = userId != null && !userId.isBlank();
        boolean hasDates = start != null && end != null;

        if (hasUser && normalizedStatus != null && hasDates) {
            return payrollRepository.findByUserIdAndStatusAndPeriodStartBetween(userId, normalizedStatus, start, end);
        }
        if (hasUser && normalizedStatus != null) {
            return payrollRepository.findByUserIdAndStatus(userId, normalizedStatus);
        }
        if (hasUser && hasDates) {
            return payrollRepository.findByUserIdAndPeriodStartBetween(userId, start, end);
        }
        if (normalizedStatus != null) {
            return payrollRepository.findByStatus(normalizedStatus);
        }
        if (hasDates) {
            return payrollRepository.findByPeriodStartBetween(start, end);
        }
        if (hasUser) {
            return payrollRepository.findByUserId(userId);
        }
        return payrollRepository.findAll();
    }

    public Payroll createPayroll(Payroll payroll) {
        normalizePayrollForSave(payroll);
        return payrollRepository.save(payroll);
    }

    public Payroll updatePayroll(Long id, Payroll payrollDetails) {
        Payroll payroll = getPayrollOrThrow(id);
        payroll.setBaseAmount(payrollDetails.getBaseAmount());
        payroll.setBonusAmount(payrollDetails.getBonusAmount());
        payroll.setDeductionAmount(payrollDetails.getDeductionAmount());
        if (payrollDetails.getRoleType() != null) {
            payroll.setRoleType(normalizeRole(payrollDetails.getRoleType()));
        }
        if (payrollDetails.getSourceType() != null) {
            payroll.setSourceType(payrollDetails.getSourceType());
        }
        if (payrollDetails.getSourceReference() != null) {
            payroll.setSourceReference(payrollDetails.getSourceReference());
        }
        if (payrollDetails.getKilograms() != null) {
            payroll.setKilograms(payrollDetails.getKilograms());
        }
        if (payrollDetails.getStatus() != null) {
            payroll.setStatus(normalizeStatus(payrollDetails.getStatus()));
        }
        payroll.setPaymentDate(payrollDetails.getPaymentDate());
        payroll.setPaymentMethod(payrollDetails.getPaymentMethod());
        payroll.setNotes(payrollDetails.getNotes());
        return payrollRepository.save(payroll);
    }

    @Transactional
    public Payroll approvePayroll(Long id) {
        return approvePayroll(id, "admin");
    }

    @Transactional
    public Payroll approvePayroll(Long id, String adminId) {
        Payroll payroll = getPayrollOrThrow(id);
        String status = normalizeStatus(payroll.getStatus());
        if ("REJECTED".equals(status) || "PAID".equals(status)) {
            throw new IllegalStateException("Only PENDING, ACCEPTED, or APPROVED payrolls can be approved. Current status: " + status);
        }
        if (!Boolean.TRUE.equals(payroll.getWalletSettled())) {
            double transferred = walletService.transfer(
                    adminId == null || adminId.isBlank() ? "admin" : adminId,
                    payroll.getUserId(),
                    payroll.getTotalAmount());
            payroll.setWalletTransferAmount(transferred);
            payroll.setWalletSettled(true);
        }
        payroll.setStatus("APPROVED");
        if (payroll.getApprovedAt() == null) {
            payroll.setApprovedAt(LocalDateTime.now());
        }
        payroll.setApprovedBy(adminId == null || adminId.isBlank() ? "admin" : adminId);
        return payrollRepository.save(payroll);
    }

    public Payroll acceptPayroll(Long id) {
        Payroll payroll = getPayrollOrThrow(id);
        String status = normalizeStatus(payroll.getStatus());
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Only PENDING payrolls can be accepted. Current status: " + status);
        }
        payroll.setStatus("ACCEPTED");
        return payrollRepository.save(payroll);
    }

    public Payroll rejectPayroll(Long id, String reason) {
        Payroll payroll = getPayrollOrThrow(id);
        String status = normalizeStatus(payroll.getStatus());
        if ("REJECTED".equals(status)) {
            return payroll;
        }
        if (!"PENDING".equals(status) && !"ACCEPTED".equals(status)) {
            throw new IllegalStateException("Only PENDING or ACCEPTED payrolls can be rejected. Current status: " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        payroll.setStatus("REJECTED");
        payroll.setRejectionReason(reason);
        payroll.setRejectedAt(LocalDateTime.now());
        payroll.setNotes(reason);
        return payrollRepository.save(payroll);
    }

    public Payroll markAsPaid(Long id, String paymentMethod) {
        Payroll payroll = getPayrollOrThrow(id);
        String status = normalizeStatus(payroll.getStatus());
        if ("PAID".equals(status)) {
            return payroll;
        }
        if (!"APPROVED".equals(status)) {
            throw new IllegalStateException("Only APPROVED payrolls can be marked as paid. Current status: " + status);
        }
        payroll.setStatus("PAID");
        payroll.setPaymentDate(LocalDateTime.now());
        payroll.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "SANDBOX" : paymentMethod.toUpperCase());
        return payrollRepository.save(payroll);
    }

    public void deletePayroll(Long id) {
        payrollRepository.delete(getPayrollOrThrow(id));
    }

    // === Event-driven Payroll Generation ===

    public void processHarvestPayroll(HarvestEvent event) {
        double kilograms = firstPositive(event.getKilograms(), event.getTotalKg());
        double amount = amountFromEventOrFormula(event, "BURUH", kilograms);
        processPayrollEvent(
                eventKey(event.getEventId(), "BURUH", event.getEmployeeId()),
                event.getEmployeeId(),
                "BURUH",
                amount,
                kilograms,
                "HARVEST",
                sourceReference(event),
                "HarvestEvent",
                eventTimeOrNow(event));
    }

    public void processShipmentPayroll(ShipmentEvent event) {
        if (hasText(event.getDriverId())) {
            double driverKg = firstPositive(event.getTotalKg(), event.getKilograms());
            processPayrollEvent(
                    eventKey(event.getEventId(), "SUPIR", event.getDriverId()),
                    event.getDriverId(),
                    "SUPIR",
                    amountFromEventOrFormula(event, "SUPIR", driverKg),
                    driverKg,
                    "SHIPMENT",
                    sourceReference(event),
                    "ShipmentEvent",
                    eventTimeOrNow(event));
        }

        if (hasText(event.getMandorId())) {
            double mandorKg = firstPositive(event.getRecognizedKg(), event.getTotalKg(), event.getKilograms());
            processPayrollEvent(
                    eventKey(event.getEventId(), "MANDOR", event.getMandorId()),
                    event.getMandorId(),
                    "MANDOR",
                    calculateWage("MANDOR", mandorKg),
                    mandorKg,
                    "SHIPMENT",
                    sourceReference(event),
                    "ShipmentEvent",
                    eventTimeOrNow(event));
        }

        if (!hasText(event.getDriverId()) && !hasText(event.getMandorId())) {
            String role = hasText(event.getRoleType()) ? event.getRoleType() : "SUPIR";
            double kilograms = firstPositive(event.getKilograms(), event.getTotalKg(), event.getRecognizedKg());
            double amount = amountFromEventOrFormula(event, role, kilograms);
            processPayrollEvent(
                    eventKey(event.getEventId(), role, event.getEmployeeId()),
                    event.getEmployeeId(),
                    role,
                    amount,
                    kilograms,
                    "SHIPMENT",
                    sourceReference(event),
                    "ShipmentEvent",
                    eventTimeOrNow(event));
        }
    }

    private void processPayrollEvent(
            String eventId,
            String userId,
            String roleType,
            double amount,
            double kilograms,
            String sourceType,
            String sourceReference,
            String eventType,
            LocalDateTime eventTime) {
        if (!hasText(eventId)) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (!hasText(userId)) {
            throw new IllegalArgumentException("employeeId is required");
        }
        if (payrollRepository.findByEventId(eventId) != null) {
            return;
        }
        Payroll payroll = new Payroll();
        payroll.setUserId(userId);
        payroll.setRoleType(normalizeRole(roleType));
        payroll.setSourceType(sourceType);
        payroll.setSourceReference(sourceReference);
        payroll.setKilograms(kilograms > 0 ? kilograms : null);
        payroll.setBaseAmount(amount);
        payroll.setBonusAmount(0.0);
        payroll.setDeductionAmount(0.0);
        payroll.setStatus("PENDING");
        payroll.setPeriodStart(eventTime);
        payroll.setPeriodEnd(eventTime);
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

    private void normalizePayrollForSave(Payroll payroll) {
        if (!hasText(payroll.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }
        payroll.setStatus(payroll.getStatus() == null ? "PENDING" : normalizeStatus(payroll.getStatus()));
        if (payroll.getRoleType() != null) {
            payroll.setRoleType(normalizeRole(payroll.getRoleType()));
        }
        if (payroll.getBonusAmount() == null) {
            payroll.setBonusAmount(0.0);
        }
        if (payroll.getDeductionAmount() == null) {
            payroll.setDeductionAmount(0.0);
        }
        if (payroll.getBaseAmount() == null) {
            payroll.setBaseAmount(0.0);
        }
        if (payroll.getWalletSettled() == null) {
            payroll.setWalletSettled(false);
        }
    }

    private double amountFromEventOrFormula(com.mysawit.payroll.event.PayrollEvent event, String roleType, double kilograms) {
        if (kilograms > 0) {
            return calculateWage(roleType, kilograms);
        }
        if (event.getAmount() > 0) {
            return event.getAmount();
        }
        throw new IllegalArgumentException("Event must include kilograms or positive amount");
    }

    private double calculateWage(String roleType, double kilograms) {
        if (kilograms <= 0) {
            throw new IllegalArgumentException("kilograms must be greater than zero");
        }
        WageConfig config = wageConfigService.getActiveConfigForRole(roleType)
                .orElseThrow(() -> new IllegalStateException("No active wage config for role: " + roleType));
        return config.getRatePerKg() * kilograms * 0.9;
    }

    private String normalizeRole(String roleType) {
        if (!hasText(roleType)) {
            throw new IllegalArgumentException("roleType is required");
        }
        String upper = roleType.toUpperCase();
        if (!upper.equals("BURUH") && !upper.equals("SUPIR") && !upper.equals("MANDOR")) {
            throw new IllegalArgumentException("Invalid roleType: " + roleType);
        }
        return upper;
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            throw new IllegalArgumentException("status is required");
        }
        String upper = status.toUpperCase();
        if (!List.of("PENDING", "ACCEPTED", "APPROVED", "REJECTED", "PAID").contains(upper)) {
            throw new IllegalArgumentException("Invalid payroll status: " + status);
        }
        return upper;
    }

    private String eventKey(String eventId, String roleType, String userId) {
        if (!hasText(eventId)) {
            throw new IllegalArgumentException("eventId is required");
        }
        return eventId + ":" + normalizeRole(roleType) + ":" + userId;
    }

    private String sourceReference(com.mysawit.payroll.event.PayrollEvent event) {
        return hasText(event.getSourceReference()) ? event.getSourceReference() : event.getEventId();
    }

    private double firstPositive(Double... values) {
        for (Double value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return 0.0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime eventTimeOrNow(com.mysawit.payroll.event.PayrollEvent event) {
        if (event.getOccurredAt() != null) {
            return LocalDateTime.ofInstant(event.getOccurredAt().toInstant(), ZoneId.systemDefault());
        }
        return eventTimeOrNow(event.getTimestamp());
    }

    private LocalDateTime eventTimeOrNow(long timestamp) {
        if (timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}

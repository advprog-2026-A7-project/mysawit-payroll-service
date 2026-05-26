package com.mysawit.payroll.service;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.client.IdentityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired(required = false)
    private WageConfigRepository wageConfigRepository;

    @Autowired(required = false)
    private WalletService walletService;

    @Autowired(required = false)
    private IdentityClient identityClient;

    @Value("${identity.service.token:}")
    private String identityServiceToken;

    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    public List<Payroll> searchPayrolls(String userId, String status, LocalDateTime from, LocalDateTime to) {
        if (isBlank(userId) && isBlank(status) && from == null && to == null) {
            return payrollRepository.findAll();
        }
        return payrollRepository.search(blankToNull(userId), normalizeStatusOrNull(status), from, to);
    }

    public Optional<Payroll> getPayrollById(Long id) {
        return payrollRepository.findById(id);
    }

    public List<Payroll> getPayrollsByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(String.valueOf(employeeId));
    }

    public List<Payroll> getPayrollsByUser(String userId) {
        return payrollRepository.findByUserId(userId);
    }

    public List<Payroll> getPayrollsByStatus(String status) {
        return payrollRepository.findByStatus(status);
    }

    @Transactional
    public Payroll createPayroll(Payroll payroll) {
        normalizePayroll(payroll);
        return payrollRepository.save(payroll);
    }

    @Transactional
    public Payroll updatePayroll(Long id, Payroll payrollDetails) {
        Payroll payroll = getPayrollOrThrow(id);
        if (payrollDetails.getUserId() != null) {
            payroll.setUserId(payrollDetails.getUserId());
        }
        if (payrollDetails.getEmployeeId() != null) {
            payroll.setEmployeeId(payrollDetails.getEmployeeId());
        }
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
        if (payrollDetails.getPeriodStart() != null) {
            payroll.setPeriodStart(payrollDetails.getPeriodStart());
        }
        if (payrollDetails.getPeriodEnd() != null) {
            payroll.setPeriodEnd(payrollDetails.getPeriodEnd());
        }
        payroll.setBaseAmount(payrollDetails.getBaseAmount());
        payroll.setBonusAmount(payrollDetails.getBonusAmount());
        payroll.setDeductionAmount(payrollDetails.getDeductionAmount());
        if (payrollDetails.getStatus() != null) {
            payroll.setStatus(normalizeStatusOrNull(payrollDetails.getStatus()));
        }
        payroll.setPaymentDate(payrollDetails.getPaymentDate());
        payroll.setPaymentMethod(payrollDetails.getPaymentMethod());
        payroll.setNotes(payrollDetails.getNotes());
        normalizePayroll(payroll);
        return payrollRepository.save(payroll);
    }

    public Payroll approvePayroll(Long id) {
        Payroll payroll = getPayrollOrThrow(id);
        payroll.setStatus("APPROVED");
        return payrollRepository.save(payroll);
    }

    @Transactional
    public Payroll approvePayroll(Long id, String adminId) {
        if (isBlank(adminId)) {
            return approvePayroll(id);
        }

        Payroll payroll = getPayrollOrThrow(id);
        if (!"PENDING".equals(payroll.getStatus()) && !"ACCEPTED".equals(payroll.getStatus())) {
            throw new IllegalStateException("Only PENDING payrolls can be approved. Current status: " + payroll.getStatus());
        }
        if (isBlank(payroll.getUserId())) {
            throw new IllegalStateException("Payroll userId is required for wallet settlement");
        }
        if (walletService == null) {
            throw new IllegalStateException("Wallet service is not available");
        }

        normalizePayroll(payroll);
        double totalAmount = calculateTotal(payroll);
        if (!Boolean.TRUE.equals(payroll.getWalletSettled())) {
            walletService.transferPayroll(adminId, payroll.getUserId(), totalAmount, "payroll-" + payroll.getId());
            payroll.setWalletSettled(true);
            payroll.setWalletTransferAmount(totalAmount);
        }

        payroll.setStatus("ACCEPTED");
        payroll.setApprovedBy(adminId);
        payroll.setApprovedAt(LocalDateTime.now());
        payroll.setPaymentDate(LocalDateTime.now());
        payroll.setPaymentMethod("WALLET");
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
        if (isBlank(reason)) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        payroll.setStatus("REJECTED");
        payroll.setNotes(reason.trim());
        payroll.setRejectionReason(reason.trim());
        payroll.setRejectedAt(LocalDateTime.now());
        return payrollRepository.save(payroll);
    }

    @Transactional
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

    @Transactional
    public void processHarvestPayroll(HarvestEvent event) {
        String userId = firstNonBlank(event.getHarvesterId(), event.getEmployeeId());
        double amount = resolvePayrollAmount("BURUH", event.getWeight(), event.getAmount());
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        processPayrollEvent(
                event.getEventId(),
                userId,
                "BURUH",
                "HARVEST",
                firstNonBlank(event.getHarvestId(), event.getEventId()),
                event.getWeight(),
                amount,
                occurredAt,
                "HarvestEvent"
        );
    }

    @Transactional
    public void processShipmentPayroll(ShipmentEvent event) {
        String role = normalizeRole(firstNonBlank(event.getEmployeeRole(), "SUPIR"));
        double amount = resolvePayrollAmount(role, event.getKg(), event.getAmount());
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        processPayrollEvent(
                event.getEventId(),
                event.getEmployeeId(),
                role,
                "SHIPMENT",
                firstNonBlank(event.getShipmentId(), event.getEventId()),
                event.getKg(),
                amount,
                occurredAt,
                "ShipmentEvent"
        );
    }

    @Transactional
    private void processPayrollEvent(
            String eventId,
            String userId,
            String roleType,
            String sourceType,
            String sourceReference,
            Double kilograms,
            double amount,
            LocalDateTime occurredAt,
            String eventType
    ) {
        if (payrollRepository.findByEventId(eventId) != null) {
            return;
        }
        Payroll payroll = new Payroll();
        payroll.setUserId(userId);
        payroll.setEmployeeId(userId);
        payroll.setRoleType(roleType);
        payroll.setSourceType(sourceType);
        payroll.setSourceReference(sourceReference);
        payroll.setKilograms(kilograms);
        payroll.setBaseAmount(amount);
        payroll.setBonusAmount(0.0);
        payroll.setDeductionAmount(0.0);
        payroll.setStatus("PENDING");
        payroll.setPeriodStart(occurredAt == null ? LocalDateTime.now() : occurredAt);
        payroll.setPeriodEnd(occurredAt == null ? LocalDateTime.now() : occurredAt);
        payroll.setNotes("Generated from " + eventType + ": " + eventId);
        payroll.setEventId(eventId);

        if (identityClient != null && identityServiceToken != null && !identityServiceToken.isBlank()) {
            try {
                Long legacyEmployeeId = parseLong(userId);
                if (legacyEmployeeId != null) {
                    var userDetail = identityClient.getUserById(legacyEmployeeId, "Bearer " + identityServiceToken);
                    if (userDetail != null && userDetail.get("username") != null) {
                        payroll.setNotes(payroll.getNotes() + ", user: " + userDetail.get("username"));
                    }
                }
            } catch (Exception e) {
                payroll.setNotes(payroll.getNotes() + ", user fetch failed");
            }
        }

        payrollRepository.save(payroll);
    }

    private void normalizePayroll(Payroll payroll) {
        if (isBlank(payroll.getUserId()) && !isBlank(payroll.getEmployeeId())) {
            payroll.setUserId(payroll.getEmployeeId());
        }
        if (isBlank(payroll.getEmployeeId()) && !isBlank(payroll.getUserId())) {
            payroll.setEmployeeId(payroll.getUserId());
        }
        if (payroll.getRoleType() != null) {
            payroll.setRoleType(normalizeRole(payroll.getRoleType()));
        }
        if (payroll.getStatus() == null || payroll.getStatus().isBlank()) {
            payroll.setStatus("PENDING");
        } else {
            payroll.setStatus(normalizeStatusOrNull(payroll.getStatus()));
        }
        if (payroll.getBonusAmount() == null) {
            payroll.setBonusAmount(0.0);
        }
        if (payroll.getDeductionAmount() == null) {
            payroll.setDeductionAmount(0.0);
        }
        if (payroll.getWalletSettled() == null) {
            payroll.setWalletSettled(false);
        }
        if (payroll.getPeriodStart() == null) {
            payroll.setPeriodStart(LocalDateTime.now());
        }
        if (payroll.getPeriodEnd() == null) {
            payroll.setPeriodEnd(payroll.getPeriodStart());
        }
    }

    private double resolvePayrollAmount(String roleType, Double kilograms, Double fallbackAmount) {
        if (fallbackAmount != null && fallbackAmount > 0) {
            return fallbackAmount;
        }
        if (kilograms == null || kilograms <= 0) {
            throw new IllegalArgumentException("Kilograms must be greater than 0 when event amount is missing");
        }
        if (wageConfigRepository == null) {
            throw new IllegalStateException("Wage configuration repository is not available");
        }
        return wageConfigRepository
                .findActiveConfigForRole(normalizeRole(roleType), LocalDate.now())
                .stream()
                .findFirst()
                .or(() -> wageConfigRepository.findFirstByRoleTypeOrderByEffectiveDateDesc(normalizeRole(roleType)))
                .map(config -> config.getRatePerKg() * kilograms)
                .orElseThrow(() -> new IllegalStateException("No wage config found for role: " + roleType));
    }

    private double calculateTotal(Payroll payroll) {
        double base = payroll.getBaseAmount() == null ? 0.0 : payroll.getBaseAmount();
        double bonus = payroll.getBonusAmount() == null ? 0.0 : payroll.getBonusAmount();
        double deduction = payroll.getDeductionAmount() == null ? 0.0 : payroll.getDeductionAmount();
        double total = base + bonus - deduction;
        payroll.setTotalAmount(total);
        return total;
    }

    private String normalizeRole(String roleType) {
        return roleType == null ? null : roleType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatusOrNull(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Long parseLong(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Payroll getPayrollOrThrow(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + id));
    }

    void setIdentityClient(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    void setIdentityServiceToken(String token) {
        this.identityServiceToken = token;
    }

    void setWalletService(WalletService walletService) {
        this.walletService = walletService;
    }

    void setWageConfigRepository(WageConfigRepository wageConfigRepository) {
        this.wageConfigRepository = wageConfigRepository;
    }
}

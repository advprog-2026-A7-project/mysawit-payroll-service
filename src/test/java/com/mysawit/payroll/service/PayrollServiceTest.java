package com.mysawit.payroll.service;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Payroll pendingPayroll;
    private Payroll approvedPayroll;

    @BeforeEach
    void setUp() {
        pendingPayroll = new Payroll();
        pendingPayroll.setId(1L);
        pendingPayroll.setEmployeeId(10L);
        pendingPayroll.setBaseAmount(5000000.0);
        pendingPayroll.setBonusAmount(500000.0);
        pendingPayroll.setDeductionAmount(250000.0);
        pendingPayroll.setTotalAmount(5250000.0);
        pendingPayroll.setStatus("PENDING");
        pendingPayroll.setPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        pendingPayroll.setPeriodEnd(LocalDateTime.of(2026, 1, 31, 23, 59));

        approvedPayroll = new Payroll();
        approvedPayroll.setId(2L);
        approvedPayroll.setStatus("APPROVED");
    }

    // ── getAllPayrolls ─────────────────────────────────────────────────────────

    @Test
    void getAllPayrollsReturnsList() {
        when(payrollRepository.findAll()).thenReturn(List.of(pendingPayroll));
        List<Payroll> result = payrollService.getAllPayrolls();
        assertEquals(1, result.size());
    }

    // ── getPayrollById ────────────────────────────────────────────────────────

    @Test
    void getPayrollByIdFoundReturnsOptional() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        Optional<Payroll> result = payrollService.getPayrollById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    void getPayrollByIdNotFoundReturnsEmpty() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(payrollService.getPayrollById(99L).isEmpty());
    }

    // ── getPayrollsByEmployee ─────────────────────────────────────────────────

    @Test
    void getPayrollsByEmployeeReturnsList() {
        when(payrollRepository.findByEmployeeId(10L)).thenReturn(List.of(pendingPayroll));
        List<Payroll> result = payrollService.getPayrollsByEmployee(10L);
        assertEquals(1, result.size());
    }

    // ── getPayrollsByStatus ───────────────────────────────────────────────────

    @Test
    void getPayrollsByStatusReturnsList() {
        when(payrollRepository.findByStatus("PENDING")).thenReturn(List.of(pendingPayroll));
        List<Payroll> result = payrollService.getPayrollsByStatus("PENDING");
        assertEquals(1, result.size());
    }

    // ── createPayroll ─────────────────────────────────────────────────────────

    @Test
    void createPayrollSavesAndReturns() {
        when(payrollRepository.save(any())).thenReturn(pendingPayroll);
        Payroll result = payrollService.createPayroll(pendingPayroll);
        assertEquals("PENDING", result.getStatus());
    }

    // ── updatePayroll ─────────────────────────────────────────────────────────

    @Test
    void updatePayrollUpdatesFieldsAndReturns() {
        Payroll updates = new Payroll();
        updates.setBaseAmount(6000000.0);
        updates.setBonusAmount(600000.0);
        updates.setDeductionAmount(300000.0);
        updates.setStatus("APPROVED");
        updates.setPaymentMethod("CASH");
        updates.setNotes("Updated");
        updates.setPaymentDate(LocalDateTime.now());

        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.updatePayroll(1L, updates);
        assertEquals("APPROVED", result.getStatus());
        assertEquals(6000000.0, result.getBaseAmount());
    }

    @Test
    void updatePayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.updatePayroll(99L, new Payroll()));
    }

    // ── approvePayroll ────────────────────────────────────────────────────────

    @Test
    void approvePayrollSetsStatusApproved() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.approvePayroll(1L);
        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void approvePayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.approvePayroll(99L));
    }

    // ── acceptPayroll ─────────────────────────────────────────────────────────

    @Test
    void acceptPayrollSetsStatusAccepted() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.acceptPayroll(1L);
        assertEquals("ACCEPTED", result.getStatus());
    }

    @Test
    void acceptPayrollThrowsWhenNotPending() {
        when(payrollRepository.findById(2L)).thenReturn(Optional.of(approvedPayroll));
        assertThrows(IllegalStateException.class, () -> payrollService.acceptPayroll(2L));
    }

    @Test
    void acceptPayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.acceptPayroll(99L));
    }

    // ── rejectPayroll ─────────────────────────────────────────────────────────

    @Test
    void rejectPayrollSetsStatusRejectedWithReason() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.rejectPayroll(1L, "Insufficient hours");
        assertEquals("REJECTED", result.getStatus());
        assertEquals("Insufficient hours", result.getNotes());
    }

    @Test
    void rejectPayrollSetsStatusRejectedWithoutReason() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.rejectPayroll(1L, null);
        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void rejectPayrollSetsStatusRejectedWithBlankReason() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.rejectPayroll(1L, "  ");
        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void rejectPayrollThrowsWhenNotPending() {
        when(payrollRepository.findById(2L)).thenReturn(Optional.of(approvedPayroll));
        assertThrows(IllegalStateException.class, () -> payrollService.rejectPayroll(2L, "reason"));
    }

    @Test
    void rejectPayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.rejectPayroll(99L, "reason"));
    }

    // ── markAsPaid ────────────────────────────────────────────────────────────

    @Test
    void markAsPaidSetsStatusPaid() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.markAsPaid(1L, "BANK_TRANSFER");
        assertEquals("PAID", result.getStatus());
        assertEquals("BANK_TRANSFER", result.getPaymentMethod());
        assertNotNull(result.getPaymentDate());
    }

    @Test
    void markAsPaidThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.markAsPaid(99L, "CASH"));
    }

    // ── deletePayroll ─────────────────────────────────────────────────────────

    @Test
    void deletePayrollDeletesSuccessfully() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        payrollService.deletePayroll(1L);
        verify(payrollRepository).delete(pendingPayroll);
    }

    @Test
    void deletePayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.deletePayroll(99L));
    }
    // ── processHarvestPayroll & processShipmentPayroll ───────────────

    @Mock
    private com.mysawit.payroll.client.IdentityClient identityClient;

    private HarvestEvent createHarvestEvent(String eventId, String empId, double amt) {
        HarvestEvent e = new HarvestEvent();
        e.setEventId(eventId);
        e.setEmployeeId(empId);
        e.setAmount(amt);
        return e;
    }

    private ShipmentEvent createShipmentEvent(String eventId, String empId, double amt) {
        ShipmentEvent e = new ShipmentEvent();
        e.setEventId(eventId);
        e.setEmployeeId(empId);
        e.setAmount(amt);
        return e;
    }

    @Test
    void processPayrollSaveWhenNotExists() {
        HarvestEvent event = createHarvestEvent("evt-1", "10", 10000.0);
        when(payrollRepository.findByEventId("evt-1")).thenReturn(null);
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        payrollService.setIdentityClient(null);
        payrollService.processHarvestPayroll(event);
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void processPayrollSkipWhenExists() {
        HarvestEvent event = createHarvestEvent("evt-2", "10", 10000.0);
        when(payrollRepository.findByEventId("evt-2")).thenReturn(new Payroll());
        payrollService.processHarvestPayroll(event);
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void processShipmentPayroll() {
        ShipmentEvent event = createShipmentEvent("evt-3", "11", 20000.0);
        when(payrollRepository.findByEventId("evt-3")).thenReturn(null);
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        payrollService.setIdentityClient(null);
        payrollService.processShipmentPayroll(event);
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void processPayrollWithUserSuccess() {
        HarvestEvent event = createHarvestEvent("evt-5", "12", 15000.0);
        when(payrollRepository.findByEventId("evt-5")).thenReturn(null);
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        payrollService.setIdentityClient(identityClient);
        payrollService.setIdentityServiceToken("token");
        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("username", "testuser");
        when(identityClient.getUserById(12L, "Bearer token")).thenReturn(userMap);
        payrollService.processHarvestPayroll(event);
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void processPayrollWithUserError() {
        HarvestEvent event = createHarvestEvent("evt-6", "13", 16000.0);
        when(payrollRepository.findByEventId("evt-6")).thenReturn(null);
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        payrollService.setIdentityClient(identityClient);
        payrollService.setIdentityServiceToken("token");
        when(identityClient.getUserById(13L, "Bearer token")).thenThrow(new RuntimeException("fail"));
        payrollService.processHarvestPayroll(event);
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void processPayrollWithNullUser() {
        HarvestEvent event = createHarvestEvent("evt-7", "14", 17000.0);
        when(payrollRepository.findByEventId("evt-7")).thenReturn(null);
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        payrollService.setIdentityClient(identityClient);
        payrollService.setIdentityServiceToken("token");
        when(identityClient.getUserById(14L, "Bearer token")).thenReturn(new java.util.HashMap<>());
        payrollService.processHarvestPayroll(event);
        verify(payrollRepository).save(any(Payroll.class));
    }

}

package com.mysawit.payroll.service;

import com.mysawit.payroll.PayrollTestFixtures;
import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.PayrollEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.model.UserReplica;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.UserReplicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    private static final String USER_ID = PayrollTestFixtures.SAMPLE_USER_ID;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private UserReplicaRepository userReplicaRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Payroll pendingPayroll;
    private Payroll approvedPayroll;

    @BeforeEach
    void setUp() {
        pendingPayroll = PayrollTestFixtures.pendingPayroll();

        approvedPayroll = new Payroll();
        approvedPayroll.setId(2L);
        approvedPayroll.setStatus("APPROVED");
    }

    // ── getAllPayrolls ─────────────────────────────────────────────────────────

    @Test
    void getAllPayrollsReturnsList() {
        when(payrollRepository.findAll()).thenReturn(List.of(pendingPayroll));
        assertEquals(1, payrollService.getAllPayrolls().size());
    }

    // ── getPayrollById ────────────────────────────────────────────────────────

    @Test
    void getPayrollByIdFoundReturnsOptional() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        assertTrue(payrollService.getPayrollById(1L).isPresent());
    }

    @Test
    void getPayrollByIdNotFoundReturnsEmpty() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(payrollService.getPayrollById(99L).isEmpty());
    }

    // ── getPayrollsByUser ─────────────────────────────────────────────────────

    @Test
    void getPayrollsByUserReturnsList() {
        when(payrollRepository.findByUserId(USER_ID)).thenReturn(List.of(pendingPayroll));
        assertEquals(1, payrollService.getPayrollsByUser(USER_ID).size());
    }

    // ── getPayrollsByStatus ───────────────────────────────────────────────────

    @Test
    void getPayrollsByStatusReturnsList() {
        when(payrollRepository.findByStatus("PENDING")).thenReturn(List.of(pendingPayroll));
        assertEquals(1, payrollService.getPayrollsByStatus("PENDING").size());
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

    // ── processHarvestPayroll & processShipmentPayroll ────────────────────────

    private <T extends PayrollEvent> T createEvent(T event, String eventId, String userId, double amt) {
        event.setEventId(eventId);
        event.setEmployeeId(userId);
        event.setAmount(amt);
        return event;
    }

    private HarvestEvent createHarvestEvent(String eventId, String userId, double amt) {
        return createEvent(new HarvestEvent(), eventId, userId, amt);
    }

    private ShipmentEvent createShipmentEvent(String eventId, String userId, double amt) {
        return createEvent(new ShipmentEvent(), eventId, userId, amt);
    }

    @Test
    void processHarvestPayrollSavesWhenEventIdNotSeen() {
        HarvestEvent event = createHarvestEvent("evt-1", USER_ID, 10000.0);
        when(payrollRepository.findByEventId("evt-1")).thenReturn(null);
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());

        payrollService.processHarvestPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        Payroll saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(10000.0, saved.getBaseAmount());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("evt-1", saved.getEventId());
        assertEquals("Generated from HarvestEvent: evt-1", saved.getNotes());
    }

    @Test
    void processHarvestPayrollSkipsWhenEventIdAlreadySeen() {
        HarvestEvent event = createHarvestEvent("evt-2", USER_ID, 10000.0);
        when(payrollRepository.findByEventId("evt-2")).thenReturn(new Payroll());

        payrollService.processHarvestPayroll(event);

        verify(payrollRepository, never()).save(any());
        verifyNoInteractions(userReplicaRepository);
    }

    @Test
    void processShipmentPayrollSavesWhenEventIdNotSeen() {
        ShipmentEvent event = createShipmentEvent("evt-3", USER_ID, 20000.0);
        when(payrollRepository.findByEventId("evt-3")).thenReturn(null);
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());

        payrollService.processShipmentPayroll(event);

        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void processPayrollEnrichesNotesWhenUserReplicaPresent() {
        HarvestEvent event = createHarvestEvent("evt-4", USER_ID, 15000.0);
        when(payrollRepository.findByEventId("evt-4")).thenReturn(null);
        when(userReplicaRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserReplica(USER_ID, "sari", "BURUH")));
        when(payrollRepository.save(any())).thenReturn(new Payroll());

        payrollService.processHarvestPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals("Generated from HarvestEvent: evt-4, user: sari", captor.getValue().getNotes());
    }
}

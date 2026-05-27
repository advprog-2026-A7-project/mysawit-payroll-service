package com.mysawit.payroll.service;

import com.mysawit.payroll.PayrollTestFixtures;
import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.PayrollEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.model.PayrollBrokerEvent;
import com.mysawit.payroll.model.UserReplica;
import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.PayrollBrokerEventRepository;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.UserReplicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
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
    private PayrollBrokerEventRepository payrollBrokerEventRepository;

    @Mock
    private UserReplicaRepository userReplicaRepository;

    @Mock
    private WageConfigService wageConfigService;

    @Mock
    private WalletService walletService;

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

    @Test
    void searchPayrollsCoversEveryFilterCombination() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 31, 23, 59);
        when(payrollRepository.findByUserIdAndStatusAndPeriodStartBetween(USER_ID, "PENDING", start, end))
                .thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findByUserIdAndStatus(USER_ID, "PENDING")).thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findByUserIdAndPeriodStartBetween(USER_ID, start, end)).thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findByStatus("PENDING")).thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findByPeriodStartBetween(start, end)).thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findByUserId(USER_ID)).thenReturn(List.of(pendingPayroll));
        when(payrollRepository.findAll()).thenReturn(List.of(pendingPayroll));

        assertEquals(1, payrollService.searchPayrolls(USER_ID, "pending", start, end).size());
        assertEquals(1, payrollService.searchPayrolls(USER_ID, "PENDING", null, null).size());
        assertEquals(1, payrollService.searchPayrolls(USER_ID, null, start, end).size());
        assertEquals(1, payrollService.searchPayrolls(null, "PENDING", null, null).size());
        assertEquals(1, payrollService.searchPayrolls(null, null, start, end).size());
        assertEquals(1, payrollService.searchPayrolls(USER_ID, null, null, null).size());
        assertEquals(1, payrollService.searchPayrolls(null, null, null, null).size());
    }

    // ── createPayroll ─────────────────────────────────────────────────────────

    @Test
    void createPayrollSavesAndReturns() {
        when(payrollRepository.save(any())).thenReturn(pendingPayroll);
        Payroll result = payrollService.createPayroll(pendingPayroll);
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void createPayrollDefaultsNullableFieldsAndNormalizesRole() {
        Payroll payroll = new Payroll();
        payroll.setUserId(USER_ID);
        payroll.setRoleType("buruh");
        payroll.setBaseAmount(null);
        payroll.setBonusAmount(null);
        payroll.setDeductionAmount(null);
        payroll.setWalletSettled(null);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.createPayroll(payroll);

        assertEquals("PENDING", result.getStatus());
        assertEquals("BURUH", result.getRoleType());
        assertEquals(0.0, result.getBaseAmount());
        assertEquals(0.0, result.getBonusAmount());
        assertEquals(0.0, result.getDeductionAmount());
        assertFalse(result.getWalletSettled());
    }

    @Test
    void createPayrollRejectsMissingUserId() {
        Payroll payroll = new Payroll();
        assertThrows(IllegalArgumentException.class, () -> payrollService.createPayroll(payroll));
    }

    // ── updatePayroll ─────────────────────────────────────────────────────────

    @Test
    void updatePayrollUpdatesFieldsAndReturns() {
        Payroll updates = new Payroll();
        updates.setBaseAmount(6000000.0);
        updates.setBonusAmount(600000.0);
        updates.setDeductionAmount(300000.0);
        updates.setStatus("APPROVED");
        updates.setRoleType("mandor");
        updates.setSourceType("SHIPMENT");
        updates.setSourceReference("shipment-1");
        updates.setKilograms(80.0);
        updates.setPaymentMethod("CASH");
        updates.setNotes("Updated");
        updates.setPaymentDate(LocalDateTime.now());

        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.updatePayroll(1L, updates);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("MANDOR", result.getRoleType());
        assertEquals("SHIPMENT", result.getSourceType());
        assertEquals("shipment-1", result.getSourceReference());
        assertEquals(80.0, result.getKilograms());
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
        when(walletService.transfer("admin", USER_ID, 5250000.0)).thenReturn(5250000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.approvePayroll(1L);
        assertEquals("APPROVED", result.getStatus());
        assertTrue(result.getWalletSettled());
        assertEquals(5250000.0, result.getWalletTransferAmount());
    }

    @Test
    void approvePayrollThrowsWhenNotFound() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> payrollService.approvePayroll(99L));
    }

    @Test
    void approvePayrollRejectsRejectedOrPaidPayrolls() {
        Payroll rejected = PayrollTestFixtures.pendingPayroll();
        rejected.setStatus("REJECTED");
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(rejected));
        assertThrows(IllegalStateException.class, () -> payrollService.approvePayroll(1L));

        Payroll paid = PayrollTestFixtures.pendingPayroll();
        paid.setStatus("PAID");
        when(payrollRepository.findById(2L)).thenReturn(Optional.of(paid));
        assertThrows(IllegalStateException.class, () -> payrollService.approvePayroll(2L));
    }

    @Test
    void approvePayrollDoesNotTransferWalletTwiceAndDefaultsBlankAdmin() {
        pendingPayroll.setWalletSettled(true);
        pendingPayroll.setApprovedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.approvePayroll(1L, " ");

        assertEquals("admin", result.getApprovedBy());
        verifyNoInteractions(walletService);
    }

    @Test
    void approvePayrollUsesDefaultAdminForWalletTransferWhenAdminBlank() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        when(walletService.transfer("admin", USER_ID, 5250000.0)).thenReturn(5250000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payroll result = payrollService.approvePayroll(1L, " ");

        assertEquals("admin", result.getApprovedBy());
        assertEquals(5250000.0, result.getWalletTransferAmount());
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
        assertThrows(IllegalArgumentException.class, () -> payrollService.rejectPayroll(1L, null));
    }

    @Test
    void rejectPayrollSetsStatusRejectedWithBlankReason() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        assertThrows(IllegalArgumentException.class, () -> payrollService.rejectPayroll(1L, "  "));
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

    @Test
    void rejectPayrollReturnsAlreadyRejectedPayrollIdempotently() {
        Payroll rejected = PayrollTestFixtures.pendingPayroll();
        rejected.setStatus("REJECTED");
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(rejected));

        Payroll result = payrollService.rejectPayroll(1L, "reason");

        assertSame(rejected, result);
        verify(payrollRepository, never()).save(any());
    }

    // ── markAsPaid ────────────────────────────────────────────────────────────

    @Test
    void markAsPaidSetsStatusPaid() {
        pendingPayroll.setStatus("APPROVED");
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

    @Test
    void markAsPaidIsIdempotentWhenAlreadyPaid() {
        pendingPayroll.setStatus("PAID");
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));

        Payroll result = payrollService.markAsPaid(1L, "CASH");

        assertSame(pendingPayroll, result);
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void markAsPaidRejectsNonApprovedPayrollAndDefaultsBlankMethod() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(pendingPayroll));
        assertThrows(IllegalStateException.class, () -> payrollService.markAsPaid(1L, "CASH"));

        approvedPayroll.setUserId(USER_ID);
        when(payrollRepository.findById(2L)).thenReturn(Optional.of(approvedPayroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Payroll result = payrollService.markAsPaid(2L, " ");
        assertEquals("SANDBOX", result.getPaymentMethod());
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
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenAnswer(inv -> {
            Payroll payroll = inv.getArgument(0);
            payroll.setId(10L);
            return payroll;
        });
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processHarvestPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        Payroll saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(10000.0, saved.getBaseAmount());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("BURUH", saved.getRoleType());
        assertEquals("HARVEST", saved.getSourceType());
        assertEquals("Generated from HarvestEvent: evt-1:BURUH:" + USER_ID, saved.getNotes());

        ArgumentCaptor<PayrollBrokerEvent> eventCaptor = ArgumentCaptor.forClass(PayrollBrokerEvent.class);
        verify(payrollBrokerEventRepository).save(eventCaptor.capture());
        PayrollBrokerEvent savedEvent = eventCaptor.getValue();
        assertEquals("evt-1:BURUH:" + USER_ID, savedEvent.getEventKey());
        assertEquals("evt-1", savedEvent.getEventId());
        assertEquals("HarvestEvent", savedEvent.getEventType());
        assertEquals("HARVEST", savedEvent.getSourceType());
        assertEquals(USER_ID, savedEvent.getUserId());
        assertEquals("BURUH", savedEvent.getRoleType());
        assertEquals(10000.0, savedEvent.getAmount());
        assertEquals(10L, savedEvent.getPayrollId());
    }

    @Test
    void processHarvestPayrollSkipsWhenEventIdAlreadySeen() {
        HarvestEvent event = createHarvestEvent("evt-2", USER_ID, 10000.0);
        when(payrollBrokerEventRepository.existsByEventKey("evt-2:BURUH:" + USER_ID)).thenReturn(true);

        payrollService.processHarvestPayroll(event);

        verify(payrollRepository, never()).save(any());
        verify(payrollBrokerEventRepository, never()).save(any());
        verifyNoInteractions(userReplicaRepository);
    }

    @Test
    void processShipmentPayrollSavesWhenEventIdNotSeen() {
        ShipmentEvent event = createShipmentEvent("evt-3", USER_ID, 20000.0);
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processShipmentPayroll(event);

        verify(payrollRepository).save(any(Payroll.class));
        verify(payrollBrokerEventRepository).save(any(PayrollBrokerEvent.class));
    }

    @Test
    void processShipmentPayrollCreatesDriverAndMandorPayrolls() {
        ShipmentEvent event = new ShipmentEvent();
        event.setEventId("shipment-event");
        event.setDriverId("driver-1");
        event.setMandorId("mandor-1");
        event.setTotalKg(100.0);
        event.setRecognizedKg(80.0);
        event.setSourceReference("shipment-001");
        event.setTimestamp(1779400000000L);
        when(wageConfigService.getActiveConfigForRole("SUPIR"))
                .thenReturn(Optional.of(new WageConfig("SUPIR", 250.0, java.time.LocalDate.now())));
        when(wageConfigService.getActiveConfigForRole("MANDOR"))
                .thenReturn(Optional.of(new WageConfig("MANDOR", 150.0, java.time.LocalDate.now())));
        when(userReplicaRepository.findById(any())).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processShipmentPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository, times(2)).save(captor.capture());
        assertEquals("SUPIR", captor.getAllValues().get(0).getRoleType());
        assertEquals(22500.0, captor.getAllValues().get(0).getBaseAmount());
        assertEquals("MANDOR", captor.getAllValues().get(1).getRoleType());
        assertEquals(10800.0, captor.getAllValues().get(1).getBaseAmount());
        assertEquals("shipment-001", captor.getAllValues().get(0).getSourceReference());
        verify(payrollBrokerEventRepository, times(2)).save(any(PayrollBrokerEvent.class));
    }

    @Test
    void processShipmentPayrollUsesRoleTypeFallbackWhenNoDriverOrMandor() {
        ShipmentEvent event = new ShipmentEvent();
        event.setEventId("shipment-fallback");
        event.setEmployeeId(USER_ID);
        event.setRoleType("MANDOR");
        event.setRecognizedKg(50.0);
        when(wageConfigService.getActiveConfigForRole("MANDOR"))
                .thenReturn(Optional.of(new WageConfig("MANDOR", 150.0, java.time.LocalDate.now())));
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processShipmentPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals("MANDOR", captor.getValue().getRoleType());
        assertEquals(6750.0, captor.getValue().getBaseAmount());
    }

    @Test
    void processPayrollEnrichesNotesWhenUserReplicaPresent() {
        HarvestEvent event = createHarvestEvent("evt-4", USER_ID, 15000.0);
        when(userReplicaRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserReplica(USER_ID, "sari", "BURUH")));
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processHarvestPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals("Generated from HarvestEvent: evt-4:BURUH:" + USER_ID + ", user: sari", captor.getValue().getNotes());
    }

    @Test
    void processHarvestPayrollUsesNinetyPercentFormulaWhenKilogramsPresent() {
        HarvestEvent event = createHarvestEvent("evt-5", USER_ID, 0.0);
        event.setKilograms(100.0);
        WageConfig wageConfig = new WageConfig("BURUH", 350.0, java.time.LocalDate.now());
        when(wageConfigService.getActiveConfigForRole("BURUH")).thenReturn(Optional.of(wageConfig));
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenReturn(new Payroll());
        when(payrollBrokerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.processHarvestPayroll(event);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals(31500.0, captor.getValue().getBaseAmount());
        assertEquals(100.0, captor.getValue().getKilograms());
    }

    @Test
    void processHarvestPayrollRejectsInvalidEventsAndMissingWageConfig() {
        HarvestEvent missingEmployee = createHarvestEvent("evt-invalid", " ", 1000.0);
        assertThrows(IllegalArgumentException.class, () -> payrollService.processHarvestPayroll(missingEmployee));

        HarvestEvent missingEventId = createHarvestEvent(" ", USER_ID, 1000.0);
        assertThrows(IllegalArgumentException.class, () -> payrollService.processHarvestPayroll(missingEventId));

        HarvestEvent missingAmount = createHarvestEvent("evt-no-amount", USER_ID, 0.0);
        assertThrows(IllegalArgumentException.class, () -> payrollService.processHarvestPayroll(missingAmount));

        HarvestEvent missingConfig = createHarvestEvent("evt-no-config", USER_ID, 0.0);
        missingConfig.setKilograms(10.0);
        when(wageConfigService.getActiveConfigForRole("BURUH")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> payrollService.processHarvestPayroll(missingConfig));
    }

    @Test
    void createPayrollRejectsInvalidRoleAndStatus() {
        Payroll invalidRole = new Payroll();
        invalidRole.setUserId(USER_ID);
        invalidRole.setRoleType("BAD");
        assertThrows(IllegalArgumentException.class, () -> payrollService.createPayroll(invalidRole));

        Payroll invalidStatus = new Payroll();
        invalidStatus.setUserId(USER_ID);
        invalidStatus.setStatus("UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> payrollService.createPayroll(invalidStatus));

        Payroll blankRole = new Payroll();
        blankRole.setUserId(USER_ID);
        blankRole.setRoleType(" ");
        assertThrows(IllegalArgumentException.class, () -> payrollService.createPayroll(blankRole));

        assertThrows(IllegalArgumentException.class, () -> payrollService.getPayrollsByStatus(" "));
    }

    @Test
    void privateValidationGuardsRemainCovered() throws Exception {
        Method processPayrollEvent = PayrollService.class.getDeclaredMethod(
                "processPayrollEvent",
                String.class,
                String.class,
                String.class,
                String.class,
                double.class,
                double.class,
                String.class,
                String.class,
                String.class,
                LocalDateTime.class);
        processPayrollEvent.setAccessible(true);

        Exception missingEventId = assertThrows(Exception.class, () -> processPayrollEvent.invoke(
                payrollService,
                " ",
                "evt-reflect",
                USER_ID,
                "BURUH",
                100.0,
                1.0,
                "HARVEST",
                "harvest-1",
                "HarvestEvent",
                LocalDateTime.now()));
        assertTrue(missingEventId.getCause() instanceof IllegalArgumentException);

        Method calculateWage = PayrollService.class.getDeclaredMethod("calculateWage", String.class, double.class);
        calculateWage.setAccessible(true);
        Exception invalidKg = assertThrows(Exception.class, () -> calculateWage.invoke(payrollService, "BURUH", 0.0));
        assertTrue(invalidKg.getCause() instanceof IllegalArgumentException);
    }
}

package com.mysawit.payroll.controller;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mysawit.payroll.PayrollTestFixtures;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollControllerTest {

    @Mock
    private PayrollService payrollService;

    @InjectMocks
    private PayrollController payrollController;

    private Payroll pendingPayroll;

    @BeforeEach
    void setUp() {
        pendingPayroll = PayrollTestFixtures.pendingPayroll();
    }

    // ── GET all ───────────────────────────────────────────────────────────────

    @Test
    void getAllPayrollsReturns200() {
        when(payrollService.searchPayrolls(null, null, null, null)).thenReturn(List.of(pendingPayroll));
        ResponseEntity<List<Payroll>> response = payrollController.getAllPayrolls(null, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @Test
    void getPayrollByIdFoundReturns200() {
        when(payrollService.getPayrollById(1L)).thenReturn(Optional.of(pendingPayroll));
        ResponseEntity<Payroll> response = payrollController.getPayrollById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPayrollByIdNotFoundReturns404() {
        when(payrollService.getPayrollById(99L)).thenReturn(Optional.empty());
        ResponseEntity<Payroll> response = payrollController.getPayrollById(99L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── GET by user ───────────────────────────────────────────────────────────

    @Test
    void getPayrollsByUserReturns200() {
        String userId = com.mysawit.payroll.PayrollTestFixtures.SAMPLE_USER_ID;
        when(payrollService.getPayrollsByUser(userId)).thenReturn(List.of(pendingPayroll));
        ResponseEntity<List<Payroll>> response = payrollController.getPayrollsByUser(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── GET by status ─────────────────────────────────────────────────────────

    @Test
    void getPayrollsByStatusReturns200() {
        when(payrollService.getPayrollsByStatus("PENDING")).thenReturn(List.of(pendingPayroll));
        ResponseEntity<List<Payroll>> response = payrollController.getPayrollsByStatus("PENDING");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── POST create ───────────────────────────────────────────────────────────

    @Test
    void createPayrollReturns201() {
        when(payrollService.createPayroll(any())).thenReturn(pendingPayroll);
        ResponseEntity<Payroll> response = payrollController.createPayroll(pendingPayroll);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void processDemoHarvestEventReturns200AndDelegatesToService() {
        HarvestEvent event = new HarvestEvent();
        event.setEventId("harvest-demo-001");
        event.setEmployeeId(PayrollTestFixtures.SAMPLE_USER_ID);
        event.setAmount(1250000.0);

        ResponseEntity<Map<String, String>> response = payrollController.processDemoHarvestEvent(event);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("harvest-demo-001", response.getBody().get("eventId"));
        verify(payrollService).processHarvestPayroll(event);
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    void updatePayrollReturns200OnSuccess() {
        when(payrollService.updatePayroll(eq(1L), any())).thenReturn(pendingPayroll);
        ResponseEntity<Payroll> response = payrollController.updatePayroll(1L, pendingPayroll);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updatePayrollReturns404WhenNotFound() {
        when(payrollService.updatePayroll(eq(99L), any()))
                .thenThrow(new RuntimeException("not found"));
        ResponseEntity<Payroll> response = payrollController.updatePayroll(99L, pendingPayroll);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── PATCH approve ─────────────────────────────────────────────────────────

    @Test
    void approvePayrollReturns200OnSuccess() {
        Payroll approved = new Payroll();
        approved.setStatus("APPROVED");
        when(payrollService.approvePayroll(1L, "admin-demo")).thenReturn(approved);
        ResponseEntity<?> response = payrollController.approvePayroll(1L, Map.of("adminId", "admin-demo"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void approvePayrollReturns404WhenNotFound() {
        when(payrollService.approvePayroll(99L, null)).thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> response = payrollController.approvePayroll(99L, null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── PATCH accept ──────────────────────────────────────────────────────────

    @Test
    void acceptPayrollReturns200OnSuccess() {
        Payroll accepted = new Payroll();
        accepted.setStatus("ACCEPTED");
        when(payrollService.acceptPayroll(1L)).thenReturn(accepted);
        ResponseEntity<?> response = payrollController.acceptPayroll(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void acceptPayrollReturns400WhenNotPending() {
        when(payrollService.acceptPayroll(1L))
                .thenThrow(new IllegalStateException("Only PENDING payrolls can be accepted"));
        ResponseEntity<?> response = payrollController.acceptPayroll(1L);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void acceptPayrollReturns404WhenNotFound() {
        when(payrollService.acceptPayroll(99L)).thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> response = payrollController.acceptPayroll(99L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── PATCH reject ──────────────────────────────────────────────────────────

    @Test
    void rejectPayrollReturns200WithReason() {
        Payroll rejected = new Payroll();
        rejected.setStatus("REJECTED");
        when(payrollService.rejectPayroll(eq(1L), any())).thenReturn(rejected);

        Map<String, String> body = new HashMap<>();
        body.put("reason", "Insufficient hours");
        ResponseEntity<?> response = payrollController.rejectPayroll(1L, body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void rejectPayrollReturns200WithNullBody() {
        Payroll rejected = new Payroll();
        rejected.setStatus("REJECTED");
        when(payrollService.rejectPayroll(eq(1L), eq(null))).thenReturn(rejected);

        ResponseEntity<?> response = payrollController.rejectPayroll(1L, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void rejectPayrollReturns400WhenNotPending() {
        when(payrollService.rejectPayroll(eq(1L), any()))
                .thenThrow(new IllegalStateException("Only PENDING payrolls can be rejected"));
        ResponseEntity<?> response = payrollController.rejectPayroll(1L, Map.of("reason", "x"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void rejectPayrollReturns404WhenNotFound() {
        when(payrollService.rejectPayroll(eq(99L), any()))
                .thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> response = payrollController.rejectPayroll(99L, Map.of());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── PATCH pay ─────────────────────────────────────────────────────────────

    @Test
    void markAsPaidReturns200OnSuccess() {
        Payroll paid = new Payroll();
        paid.setStatus("PAID");
        when(payrollService.markAsPaid(eq(1L), any())).thenReturn(paid);
        ResponseEntity<?> response = payrollController.markAsPaid(1L, Map.of("paymentMethod", "CASH"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void markAsPaidUsesDefaultPaymentMethod() {
        Payroll paid = new Payroll();
        paid.setStatus("PAID");
        when(payrollService.markAsPaid(eq(1L), eq("SANDBOX"))).thenReturn(paid);
        ResponseEntity<?> response = payrollController.markAsPaid(1L, Map.of());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void markAsPaidReturns404WhenNotFound() {
        when(payrollService.markAsPaid(eq(99L), any())).thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> response = payrollController.markAsPaid(99L, Map.of("paymentMethod", "CASH"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deletePayrollReturns204OnSuccess() {
        doNothing().when(payrollService).deletePayroll(1L);
        ResponseEntity<Void> response = payrollController.deletePayroll(1L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deletePayrollReturns404WhenNotFound() {
        doThrow(new RuntimeException("not found")).when(payrollService).deletePayroll(99L);
        ResponseEntity<Void> response = payrollController.deletePayroll(99L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

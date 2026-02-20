package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollControllerTest {

    private PayrollService payrollService;
    private PayrollController payrollController;

    @BeforeEach
    void setUp() {
        payrollService = mock(PayrollService.class);
        payrollController = new PayrollController();
        ReflectionTestUtils.setField(payrollController, "payrollService", payrollService);
    }

    @Test
    void getAllPayrollsReturnsList() {
        when(payrollService.getAllPayrolls()).thenReturn(List.of(samplePayroll(1L), samplePayroll(2L)));

        ResponseEntity<List<Payroll>> response = payrollController.getAllPayrolls();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getPayrollByIdReturnsPayrollWhenFound() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.getPayrollById(1L)).thenReturn(Optional.of(payroll));

        ResponseEntity<Payroll> response = payrollController.getPayrollById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void getPayrollByIdReturnsNotFoundWhenMissing() {
        when(payrollService.getPayrollById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Payroll> response = payrollController.getPayrollById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getPayrollsByEmployeeReturnsList() {
        when(payrollService.getPayrollsByEmployee(10L)).thenReturn(List.of(samplePayroll(1L)));

        ResponseEntity<List<Payroll>> response = payrollController.getPayrollsByEmployee(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPayrollsByStatusReturnsList() {
        when(payrollService.getPayrollsByStatus("PENDING")).thenReturn(List.of(samplePayroll(1L)));

        ResponseEntity<List<Payroll>> response = payrollController.getPayrollsByStatus("PENDING");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createPayrollReturnsCreatedStatus() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.createPayroll(payroll)).thenReturn(payroll);

        ResponseEntity<Payroll> response = payrollController.createPayroll(payroll);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void updatePayrollReturnsUpdatedWhenFound() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.updatePayroll(1L, payroll)).thenReturn(payroll);

        ResponseEntity<Payroll> response = payrollController.updatePayroll(1L, payroll);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void updatePayrollReturnsNotFoundOnError() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.updatePayroll(1L, payroll)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<Payroll> response = payrollController.updatePayroll(1L, payroll);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void approvePayrollReturnsApprovedWhenFound() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.approvePayroll(1L)).thenReturn(payroll);

        ResponseEntity<Payroll> response = payrollController.approvePayroll(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void approvePayrollReturnsNotFoundOnError() {
        when(payrollService.approvePayroll(1L)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<Payroll> response = payrollController.approvePayroll(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void markAsPaidUsesProvidedPaymentMethod() {
        Payroll payroll = samplePayroll(1L);
        Map<String, String> request = new HashMap<>();
        request.put("paymentMethod", "CASH");
        when(payrollService.markAsPaid(1L, "CASH")).thenReturn(payroll);

        ResponseEntity<Payroll> response = payrollController.markAsPaid(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void markAsPaidUsesDefaultPaymentMethodWhenMissing() {
        Payroll payroll = samplePayroll(1L);
        when(payrollService.markAsPaid(1L, "BANK_TRANSFER")).thenReturn(payroll);

        ResponseEntity<Payroll> response = payrollController.markAsPaid(1L, Map.of());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payroll, response.getBody());
    }

    @Test
    void markAsPaidReturnsNotFoundOnError() {
        when(payrollService.markAsPaid(1L, "BANK_TRANSFER")).thenThrow(new RuntimeException("missing"));

        ResponseEntity<Payroll> response = payrollController.markAsPaid(1L, Map.of());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deletePayrollReturnsNoContentWhenFound() {
        ResponseEntity<Void> response = payrollController.deletePayroll(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(payrollService).deletePayroll(1L);
    }

    @Test
    void deletePayrollReturnsNotFoundOnError() {
        doThrow(new RuntimeException("missing")).when(payrollService).deletePayroll(1L);

        ResponseEntity<Void> response = payrollController.deletePayroll(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Payroll samplePayroll(Long id) {
        Payroll payroll = new Payroll();
        payroll.setId(id);
        payroll.setEmployeeId(10L);
        payroll.setPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        payroll.setPeriodEnd(LocalDateTime.of(2026, 1, 31, 23, 59));
        payroll.setBaseAmount(5000000.0);
        payroll.setBonusAmount(500000.0);
        payroll.setDeductionAmount(250000.0);
        payroll.setTotalAmount(5250000.0);
        payroll.setStatus("PENDING");
        payroll.setPaymentMethod("BANK_TRANSFER");
        return payroll;
    }
}

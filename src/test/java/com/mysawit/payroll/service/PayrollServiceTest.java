package com.mysawit.payroll.service;

import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollServiceTest {

    private PayrollRepository payrollRepository;
    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollRepository = mock(PayrollRepository.class);
        payrollService = new PayrollService();
        ReflectionTestUtils.setField(payrollService, "payrollRepository", payrollRepository);
    }

    @Test
    void getAllPayrollsReturnsRepositoryData() {
        when(payrollRepository.findAll()).thenReturn(List.of(new Payroll(), new Payroll()));

        assertEquals(2, payrollService.getAllPayrolls().size());
    }

    @Test
    void getPayrollByIdReturnsRepositoryData() {
        Payroll payroll = new Payroll();
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(payroll));

        assertEquals(Optional.of(payroll), payrollService.getPayrollById(1L));
    }

    @Test
    void getPayrollsByEmployeeReturnsRepositoryData() {
        when(payrollRepository.findByEmployeeId(10L)).thenReturn(List.of(new Payroll()));

        assertEquals(1, payrollService.getPayrollsByEmployee(10L).size());
    }

    @Test
    void getPayrollsByStatusReturnsRepositoryData() {
        when(payrollRepository.findByStatus("PENDING")).thenReturn(List.of(new Payroll()));

        assertEquals(1, payrollService.getPayrollsByStatus("PENDING").size());
    }

    @Test
    void createPayrollSavesEntity() {
        Payroll payroll = new Payroll();
        when(payrollRepository.save(payroll)).thenReturn(payroll);

        Payroll result = payrollService.createPayroll(payroll);

        assertSame(payroll, result);
    }

    @Test
    void updatePayrollUpdatesFieldsAndSaves() {
        Payroll existing = samplePayroll();
        Payroll details = new Payroll();
        details.setBaseAmount(6000000.0);
        details.setBonusAmount(400000.0);
        details.setDeductionAmount(100000.0);
        details.setStatus("APPROVED");
        details.setPaymentDate(LocalDateTime.of(2026, 1, 31, 12, 0));
        details.setPaymentMethod("CASH");
        details.setNotes("updated");

        when(payrollRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(payrollRepository.save(existing)).thenReturn(existing);

        Payroll result = payrollService.updatePayroll(1L, details);

        assertEquals(6000000.0, result.getBaseAmount());
        assertEquals(400000.0, result.getBonusAmount());
        assertEquals(100000.0, result.getDeductionAmount());
        assertEquals("APPROVED", result.getStatus());
        assertEquals("CASH", result.getPaymentMethod());
        assertEquals("updated", result.getNotes());
    }

    @Test
    void updatePayrollThrowsWhenMissing() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> payrollService.updatePayroll(1L, new Payroll()));

        assertEquals("Payroll not found with id: 1", exception.getMessage());
    }

    @Test
    void approvePayrollSetsStatusApproved() {
        Payroll existing = samplePayroll();
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(payrollRepository.save(existing)).thenReturn(existing);

        Payroll result = payrollService.approvePayroll(1L);

        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void approvePayrollThrowsWhenMissing() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> payrollService.approvePayroll(1L));

        assertEquals("Payroll not found with id: 1", exception.getMessage());
    }

    @Test
    void markAsPaidSetsStatusAndPaymentFields() {
        Payroll existing = samplePayroll();
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(payrollRepository.save(existing)).thenReturn(existing);

        Payroll result = payrollService.markAsPaid(1L, "BANK_TRANSFER");

        assertEquals("PAID", result.getStatus());
        assertEquals("BANK_TRANSFER", result.getPaymentMethod());
        assertNotNull(result.getPaymentDate());
    }

    @Test
    void markAsPaidThrowsWhenMissing() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> payrollService.markAsPaid(1L, "BANK_TRANSFER"));

        assertEquals("Payroll not found with id: 1", exception.getMessage());
    }

    @Test
    void deletePayrollDeletesWhenFound() {
        Payroll existing = samplePayroll();
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(existing));

        payrollService.deletePayroll(1L);

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).delete(captor.capture());
        assertSame(existing, captor.getValue());
    }

    @Test
    void deletePayrollThrowsWhenMissing() {
        when(payrollRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> payrollService.deletePayroll(1L));

        assertEquals("Payroll not found with id: 1", exception.getMessage());
    }

    private Payroll samplePayroll() {
        Payroll payroll = new Payroll();
        payroll.setId(1L);
        payroll.setEmployeeId(10L);
        payroll.setPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        payroll.setPeriodEnd(LocalDateTime.of(2026, 1, 31, 23, 59));
        payroll.setBaseAmount(5000000.0);
        payroll.setBonusAmount(500000.0);
        payroll.setDeductionAmount(250000.0);
        payroll.setStatus("PENDING");
        payroll.setPaymentMethod("BANK_TRANSFER");
        return payroll;
    }
}

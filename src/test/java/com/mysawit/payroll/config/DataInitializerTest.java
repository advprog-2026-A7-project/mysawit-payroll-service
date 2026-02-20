package com.mysawit.payroll.config;

import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.EmployeeRepository;
import com.mysawit.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private EmployeeRepository employeeRepository;
    private PayrollRepository payrollRepository;
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        payrollRepository = mock(PayrollRepository.class);

        dataInitializer = new DataInitializer();
        ReflectionTestUtils.setField(dataInitializer, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(dataInitializer, "payrollRepository", payrollRepository);
    }

    @Test
    void runCreatesDummyDataWhenNoEmployeeExists() throws Exception {
        when(employeeRepository.count()).thenReturn(0L);

        Employee savedEmployee = new Employee();
        savedEmployee.setId(1L);
        savedEmployee.setName("Budi Santoso");
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        Payroll savedPayroll = new Payroll();
        savedPayroll.setId(2L);
        savedPayroll.setTotalAmount(5250000.0);
        when(payrollRepository.save(any(Payroll.class))).thenReturn(savedPayroll);

        dataInitializer.run();

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertEquals("EMP001", employeeCaptor.getValue().getEmployeeCode());
        assertEquals("ACTIVE", employeeCaptor.getValue().getStatus());

        ArgumentCaptor<Payroll> payrollCaptor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(payrollCaptor.capture());
        assertEquals(1L, payrollCaptor.getValue().getEmployeeId());
        assertEquals("PENDING", payrollCaptor.getValue().getStatus());
        assertEquals("BANK_TRANSFER", payrollCaptor.getValue().getPaymentMethod());
    }

    @Test
    void runSkipsInitializationWhenDataExists() throws Exception {
        when(employeeRepository.count()).thenReturn(1L);

        dataInitializer.run();

        verify(employeeRepository, never()).save(any(Employee.class));
        verify(payrollRepository, never()).save(any(Payroll.class));
    }
}

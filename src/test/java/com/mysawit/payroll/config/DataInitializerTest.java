package com.mysawit.payroll.config;

import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.EmployeeRepository;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private WageConfigRepository wageConfigRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    private Employee savedEmployee;
    private Payroll savedPayroll;

    @BeforeEach
    void setUp() {
        savedEmployee = new Employee();
        savedEmployee.setId(1L);
        savedEmployee.setName("Budi Santoso");
        savedEmployee.setEmployeeCode("EMP001");
        savedEmployee.setBaseSalary(5000000.0);

        savedPayroll = new Payroll();
        savedPayroll.setId(1L);
        savedPayroll.setEmployeeId(1L);
        savedPayroll.setBaseAmount(5000000.0);
        savedPayroll.setBonusAmount(500000.0);
        savedPayroll.setDeductionAmount(250000.0);
        savedPayroll.setTotalAmount(5250000.0);
        savedPayroll.setStatus("PENDING");
    }

    @Test
    void runCreatesDummyDataWhenNoEmployeeExists() throws Exception {
        when(wageConfigRepository.count()).thenReturn(0L);
        when(employeeRepository.count()).thenReturn(0L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(payrollRepository.save(any(Payroll.class))).thenReturn(savedPayroll);
        when(wageConfigRepository.save(any(WageConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        verify(wageConfigRepository, times(3)).save(any(WageConfig.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void runSkipsInitializationWhenDataExists() throws Exception {
        when(wageConfigRepository.count()).thenReturn(3L);
        when(employeeRepository.count()).thenReturn(1L);

        dataInitializer.run();

        verify(employeeRepository, never()).save(any(Employee.class));
        verify(payrollRepository, never()).save(any(Payroll.class));
        verify(wageConfigRepository, never()).save(any(WageConfig.class));
    }

    @Test
    void runSkipsWageConfigSeedWhenConfigsExist() throws Exception {
        when(wageConfigRepository.count()).thenReturn(3L);
        when(employeeRepository.count()).thenReturn(0L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(payrollRepository.save(any(Payroll.class))).thenReturn(savedPayroll);

        dataInitializer.run();

        verify(wageConfigRepository, never()).save(any(WageConfig.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(payrollRepository).save(any(Payroll.class));
    }
}

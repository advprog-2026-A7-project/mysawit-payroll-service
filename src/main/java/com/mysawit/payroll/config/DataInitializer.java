package com.mysawit.payroll.config;

import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.model.Payroll;
import com.mysawit.payroll.repository.EmployeeRepository;
import com.mysawit.payroll.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Override
    public void run(String... args) throws Exception {
        // Check if data already exists
        if (employeeRepository.count() == 0) {
            // Create dummy employee
            Employee employee = new Employee();
            employee.setName("Budi Santoso");
            employee.setEmployeeCode("EMP001");
            employee.setPosition("Harvester");
            employee.setPhoneNumber("+62812345678");
            employee.setAddress("Jl. Sawit Raya No. 123, Riau");
            employee.setHireDate(LocalDateTime.of(2025, 1, 15, 0, 0));
            employee.setBaseSalary(5000000.0);
            employee.setStatus("ACTIVE");
            
            Employee savedEmployee = employeeRepository.save(employee);
            System.out.println("✓ Created dummy employee: " + savedEmployee.getName() + " (ID: " + savedEmployee.getId() + ")");

            // Create dummy payroll for the employee
            Payroll payroll = new Payroll();
            payroll.setEmployeeId(savedEmployee.getId());
            payroll.setPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
            payroll.setPeriodEnd(LocalDateTime.of(2026, 1, 31, 23, 59));
            payroll.setBaseAmount(5000000.0);
            payroll.setBonusAmount(500000.0);
            payroll.setDeductionAmount(250000.0);
            payroll.setStatus("PENDING");
            payroll.setPaymentMethod("BANK_TRANSFER");
            payroll.setNotes("Payroll for January 2026 - includes performance bonus");

            Payroll savedPayroll = payrollRepository.save(payroll);
            System.out.println("✓ Created dummy payroll: Period Jan 2026, Total: Rp " + savedPayroll.getTotalAmount() + " (ID: " + savedPayroll.getId() + ")");
        } else {
            System.out.println("Data already exists. Skipping initialization.");
        }
    }
}

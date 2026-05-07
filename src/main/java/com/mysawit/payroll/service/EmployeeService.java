package com.mysawit.payroll.service;

import com.mysawit.payroll.event.UserRegisteredEvent;
import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Value("${payroll.default-base-salary:0}")
    private Double defaultBaseSalary;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> getEmployeeByCode(String employeeCode) {
        return employeeRepository.findByEmployeeCode(employeeCode);
    }

    public List<Employee> getEmployeesByPlantation(Long plantationId) {
        return employeeRepository.findByPlantationId(plantationId);
    }

    public List<Employee> getEmployeesByStatus(String status) {
        return employeeRepository.findByStatus(status);
    }

    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee createEmployeeFromUserRegistered(UserRegisteredEvent event) {
        String employeeCode = resolveEmployeeCode(event);
        Optional<Employee> existingEmployee = employeeRepository.findByEmployeeCode(employeeCode);
        if (existingEmployee.isPresent()) {
            return existingEmployee.get();
        }

        Employee employee = new Employee();
        employee.setName(resolveName(event));
        employee.setEmployeeCode(employeeCode);
        employee.setPosition(resolvePosition(event));
        employee.setHireDate(LocalDateTime.now());
        employee.setBaseSalary(defaultBaseSalary != null ? defaultBaseSalary : 0.0);
        employee.setStatus("ACTIVE");

        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = getEmployeeOrThrow(id);
        employee.setName(employeeDetails.getName());
        employee.setPosition(employeeDetails.getPosition());
        employee.setPlantationId(employeeDetails.getPlantationId());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setAddress(employeeDetails.getAddress());
        employee.setBaseSalary(employeeDetails.getBaseSalary());
        employee.setStatus(employeeDetails.getStatus());
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        employeeRepository.delete(getEmployeeOrThrow(id));
    }

    private Employee getEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    private String resolveEmployeeCode(UserRegisteredEvent event) {
        if (hasText(event.getEmployeeCode())) {
            return event.getEmployeeCode();
        }
        Long userId = event.resolveUserId();
        if (userId != null) {
            return "USER-" + userId;
        }
        if (hasText(event.getUsername())) {
            return "USER-" + event.getUsername();
        }
        if (hasText(event.getEmail())) {
            return "USER-" + event.getEmail();
        }
        return "USER-UNKNOWN";
    }

    private String resolveName(UserRegisteredEvent event) {
        if (hasText(event.getFullName())) {
            return event.getFullName();
        }
        if (hasText(event.getName())) {
            return event.getName();
        }
        if (hasText(event.getUsername())) {
            return event.getUsername();
        }
        if (hasText(event.getEmail())) {
            return event.getEmail();
        }
        return "Unknown User";
    }

    private String resolvePosition(UserRegisteredEvent event) {
        if (hasText(event.getPosition())) {
            return event.getPosition();
        }
        if (hasText(event.getRole())) {
            return event.getRole();
        }
        return "USER";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.mysawit.payroll.service;

import com.mysawit.payroll.event.UserRegisteredEvent;
import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        employeeService = new EmployeeService();
        ReflectionTestUtils.setField(employeeService, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(employeeService, "defaultBaseSalary", 0.0);
    }

    @Test
    void getAllEmployeesReturnsRepositoryData() {
        when(employeeRepository.findAll()).thenReturn(List.of(new Employee(), new Employee()));

        assertEquals(2, employeeService.getAllEmployees().size());
    }

    @Test
    void getEmployeeByIdReturnsRepositoryData() {
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertEquals(Optional.of(employee), employeeService.getEmployeeById(1L));
    }

    @Test
    void getEmployeeByCodeReturnsRepositoryData() {
        Employee employee = new Employee();
        when(employeeRepository.findByEmployeeCode("EMP001")).thenReturn(Optional.of(employee));

        assertEquals(Optional.of(employee), employeeService.getEmployeeByCode("EMP001"));
    }

    @Test
    void getEmployeesByPlantationReturnsRepositoryData() {
        when(employeeRepository.findByPlantationId(10L)).thenReturn(List.of(new Employee()));

        assertEquals(1, employeeService.getEmployeesByPlantation(10L).size());
    }

    @Test
    void getEmployeesByStatusReturnsRepositoryData() {
        when(employeeRepository.findByStatus("ACTIVE")).thenReturn(List.of(new Employee()));

        assertEquals(1, employeeService.getEmployeesByStatus("ACTIVE").size());
    }

    @Test
    void createEmployeeSavesEntity() {
        Employee employee = new Employee();
        when(employeeRepository.save(employee)).thenReturn(employee);

        Employee result = employeeService.createEmployee(employee);

        assertSame(employee, result);
    }

    @Test
    void createEmployeeFromUserRegisteredReturnsExistingEmployee() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(42L);
        Employee existing = new Employee();
        existing.setEmployeeCode("USER-42");
        when(employeeRepository.findByEmployeeCode("USER-42")).thenReturn(Optional.of(existing));

        Employee result = employeeService.createEmployeeFromUserRegistered(event);

        assertSame(existing, result);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployeeFromUserRegisteredCreatesEmployee() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setId(7L);
        event.setUsername("sari");
        event.setRole("HARVESTER");
        when(employeeRepository.findByEmployeeCode("USER-7")).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.createEmployeeFromUserRegistered(event);

        assertEquals("sari", result.getName());
        assertEquals("USER-7", result.getEmployeeCode());
        assertEquals("HARVESTER", result.getPosition());
        assertEquals(0.0, result.getBaseSalary());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getHireDate());
    }

    @Test
    void updateEmployeeUpdatesFieldsAndSaves() {
        Employee existing = new Employee();
        existing.setId(1L);
        existing.setEmployeeCode("EMP001");

        Employee details = new Employee();
        details.setName("Budi");
        details.setPosition("Supervisor");
        details.setPlantationId(10L);
        details.setPhoneNumber("08123");
        details.setAddress("Riau");
        details.setBaseSalary(7000000.0);
        details.setStatus("INACTIVE");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        Employee result = employeeService.updateEmployee(1L, details);

        assertEquals("Budi", result.getName());
        assertEquals("Supervisor", result.getPosition());
        assertEquals(10L, result.getPlantationId());
        assertEquals("08123", result.getPhoneNumber());
        assertEquals("Riau", result.getAddress());
        assertEquals(7000000.0, result.getBaseSalary());
        assertEquals("INACTIVE", result.getStatus());
    }

    @Test
    void updateEmployeeThrowsWhenMissing() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> employeeService.updateEmployee(1L, new Employee()));

        assertEquals("Employee not found with id: 1", exception.getMessage());
    }

    @Test
    void deleteEmployeeDeletesWhenFound() {
        Employee existing = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(1L);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).delete(captor.capture());
        assertSame(existing, captor.getValue());
    }

    @Test
    void deleteEmployeeThrowsWhenMissing() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> employeeService.deleteEmployee(1L));

        assertEquals("Employee not found with id: 1", exception.getMessage());
    }
}

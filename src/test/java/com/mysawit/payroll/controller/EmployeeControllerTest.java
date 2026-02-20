package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.Employee;
import com.mysawit.payroll.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeControllerTest {

    private EmployeeService employeeService;
    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        employeeController = new EmployeeController();
        ReflectionTestUtils.setField(employeeController, "employeeService", employeeService);
    }

    @Test
    void getAllEmployeesReturnsList() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(sampleEmployee(1L), sampleEmployee(2L)));

        ResponseEntity<List<Employee>> response = employeeController.getAllEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getEmployeeByIdReturnsEmployeeWhenFound() {
        Employee employee = sampleEmployee(1L);
        when(employeeService.getEmployeeById(1L)).thenReturn(Optional.of(employee));

        ResponseEntity<Employee> response = employeeController.getEmployeeById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(employee, response.getBody());
    }

    @Test
    void getEmployeeByIdReturnsNotFoundWhenMissing() {
        when(employeeService.getEmployeeById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Employee> response = employeeController.getEmployeeById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getEmployeeByCodeReturnsEmployeeWhenFound() {
        Employee employee = sampleEmployee(1L);
        when(employeeService.getEmployeeByCode("EMP001")).thenReturn(Optional.of(employee));

        ResponseEntity<Employee> response = employeeController.getEmployeeByCode("EMP001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(employee, response.getBody());
    }

    @Test
    void getEmployeeByCodeReturnsNotFoundWhenMissing() {
        when(employeeService.getEmployeeByCode("EMP001")).thenReturn(Optional.empty());

        ResponseEntity<Employee> response = employeeController.getEmployeeByCode("EMP001");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getEmployeesByPlantationReturnsList() {
        when(employeeService.getEmployeesByPlantation(10L)).thenReturn(List.of(sampleEmployee(1L)));

        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByPlantation(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getEmployeesByStatusReturnsList() {
        when(employeeService.getEmployeesByStatus("ACTIVE")).thenReturn(List.of(sampleEmployee(1L)));

        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByStatus("ACTIVE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createEmployeeReturnsCreatedStatus() {
        Employee employee = sampleEmployee(1L);
        when(employeeService.createEmployee(employee)).thenReturn(employee);

        ResponseEntity<Employee> response = employeeController.createEmployee(employee);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(employee, response.getBody());
    }

    @Test
    void updateEmployeeReturnsUpdatedWhenFound() {
        Employee employee = sampleEmployee(1L);
        when(employeeService.updateEmployee(1L, employee)).thenReturn(employee);

        ResponseEntity<Employee> response = employeeController.updateEmployee(1L, employee);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(employee, response.getBody());
    }

    @Test
    void updateEmployeeReturnsNotFoundOnError() {
        Employee employee = sampleEmployee(1L);
        when(employeeService.updateEmployee(1L, employee)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<Employee> response = employeeController.updateEmployee(1L, employee);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteEmployeeReturnsNoContentWhenFound() {
        ResponseEntity<Void> response = employeeController.deleteEmployee(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void deleteEmployeeReturnsNotFoundOnError() {
        doThrow(new RuntimeException("missing")).when(employeeService).deleteEmployee(1L);

        ResponseEntity<Void> response = employeeController.deleteEmployee(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Employee sampleEmployee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Budi");
        employee.setEmployeeCode("EMP001");
        employee.setPosition("Harvester");
        employee.setBaseSalary(5000000.0);
        employee.setStatus("ACTIVE");
        return employee;
    }
}

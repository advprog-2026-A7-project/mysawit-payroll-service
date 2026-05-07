package com.mysawit.payroll.event;

import com.mysawit.payroll.service.EmployeeService;
import com.mysawit.payroll.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PayrollEventConsumerImplTest {

    private PayrollService payrollService;
    private EmployeeService employeeService;
    private PayrollEventConsumerImpl consumer;

    @BeforeEach
    void setUp() {
        payrollService = mock(PayrollService.class);
        employeeService = mock(EmployeeService.class);
        consumer = new PayrollEventConsumerImpl();
        ReflectionTestUtils.setField(consumer, "payrollService", payrollService);
        ReflectionTestUtils.setField(consumer, "employeeService", employeeService);
    }

    @Test
    void handleHarvestEventDelegatesToPayrollService() {
        HarvestEvent event = new HarvestEvent();
        event.setEventId("harvest-1");

        consumer.handleHarvestEvent(event);

        verify(payrollService).processHarvestPayroll(event);
    }

    @Test
    void handleShipmentEventDelegatesToPayrollService() {
        ShipmentEvent event = new ShipmentEvent();
        event.setEventId("shipment-1");

        consumer.handleShipmentEvent(event);

        verify(payrollService).processShipmentPayroll(event);
    }

    @Test
    void handleUserRegisteredEventDelegatesToEmployeeService() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(1L);

        consumer.handleUserRegisteredEvent(event);

        verify(employeeService).createEmployeeFromUserRegistered(event);
    }
}

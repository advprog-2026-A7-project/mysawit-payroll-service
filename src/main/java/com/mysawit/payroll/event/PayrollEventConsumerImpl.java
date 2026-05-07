package com.mysawit.payroll.event;

import com.mysawit.payroll.service.EmployeeService;
import com.mysawit.payroll.service.PayrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PayrollEventConsumerImpl implements PayrollEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PayrollEventConsumerImpl.class);

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private EmployeeService employeeService;

    @Override
    @RabbitListener(queues = "${payroll.rabbitmq.queues.harvest:payroll_queue}")
    public void handleHarvestEvent(HarvestEvent event) {
        log.info("Received harvest payroll event: {}", event.getEventId());
        payrollService.processHarvestPayroll(event);
    }

    @Override
    @RabbitListener(queues = "${payroll.rabbitmq.queues.shipment:shipment.completed}")
    public void handleShipmentEvent(ShipmentEvent event) {
        log.info("Received shipment payroll event: {}", event.getEventId());
        payrollService.processShipmentPayroll(event);
    }

    @Override
    @RabbitListener(queues = "${payroll.rabbitmq.queues.user-registered:user.registered.queue}")
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Received user registered event for user: {}", event.resolveUserId());
        employeeService.createEmployeeFromUserRegistered(event);
    }
}

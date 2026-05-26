package com.mysawit.payroll.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.mysawit.payroll.config.RabbitMqConfig;
import com.mysawit.payroll.service.PayrollService;

@Service
public class PayrollEventConsumerImpl implements PayrollEventConsumer {
    @Autowired
    private PayrollService payrollService;

    @Override
    @RabbitListener(queues = "${payroll.rabbitmq.queues.harvest:" + RabbitMqConfig.HARVEST_PAYROLL_QUEUE + "}")
    public void handleHarvestEvent(HarvestEvent event) {
        payrollService.processHarvestPayroll(event);
    }

    @Override
    @RabbitListener(queues = {
            "${payroll.rabbitmq.queues.shipment-supir:" + RabbitMqConfig.SHIPMENT_SUPIR_QUEUE + "}",
            "${payroll.rabbitmq.queues.shipment-mandor:" + RabbitMqConfig.SHIPMENT_MANDOR_QUEUE + "}"
    })
    public void handleShipmentEvent(ShipmentEvent event) {
        payrollService.processShipmentPayroll(event);
    }
}

package com.mysawit.payroll.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void payrollQueuesAreDurableAndDeadLettered() {
        Queue queue = config.harvestPayrollQueue("payroll_queue", "payroll.dlx");

        assertTrue(queue.isDurable());
        assertEquals("payroll_queue", queue.getName());
        assertEquals("payroll.dlx", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("payroll_queue.dead", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void deadLetterQueuesUseStableQueueName() {
        Queue queue = config.shipmentCompletedDeadLetterQueue("shipment.completed");

        assertTrue(queue.isDurable());
        assertEquals("shipment.completed.dlq", queue.getName());
    }

    @Test
    void deadLetterBindingsUseQueueSpecificRoutingKey() {
        Queue queue = config.userRegisteredDeadLetterQueue("user.registered.queue");
        DirectExchange exchange = config.payrollDeadLetterExchange("payroll.dlx");

        Binding binding = config.userRegisteredDeadLetterBinding(queue, exchange, "user.registered.queue");

        assertEquals("user.registered.queue.dlq", binding.getDestination());
        assertEquals("payroll.dlx", binding.getExchange());
        assertEquals("user.registered.queue.dead", binding.getRoutingKey());
    }

    @Test
    void retryInterceptorCanBeBuiltFromProperties() {
        assertNotNull(config.payrollRetryInterceptor(3, 1000, 2.0, 5000));
    }
}

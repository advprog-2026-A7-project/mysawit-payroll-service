package com.mysawit.payroll.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.mockito.Mockito.mock;

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
    void allPayrollQueuesAreDurableAndDeadLettered() {
        Queue shipment = config.shipmentCompletedQueue("shipment.completed", "payroll.dlx");
        Queue userRegistered = config.userRegisteredQueue("user.registered.queue", "payroll.dlx");

        assertEquals("shipment.completed", shipment.getName());
        assertEquals("shipment.completed.dead", shipment.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("user.registered.queue", userRegistered.getName());
        assertEquals("user.registered.queue.dead", userRegistered.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void allDeadLetterQueuesUseStableNames() {
        assertEquals("payroll_queue.dlq", config.harvestPayrollDeadLetterQueue("payroll_queue").getName());
        assertEquals("user.registered.queue.dlq", config.userRegisteredDeadLetterQueue("user.registered.queue").getName());
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
    void allDeadLetterBindingsUseQueueSpecificRoutingKeys() {
        DirectExchange exchange = config.payrollDeadLetterExchange("payroll.dlx");

        Binding harvest = config.harvestPayrollDeadLetterBinding(
                config.harvestPayrollDeadLetterQueue("payroll_queue"),
                exchange,
                "payroll_queue");
        Binding shipment = config.shipmentCompletedDeadLetterBinding(
                config.shipmentCompletedDeadLetterQueue("shipment.completed"),
                exchange,
                "shipment.completed");

        assertEquals("payroll_queue.dead", harvest.getRoutingKey());
        assertEquals("shipment.completed.dead", shipment.getRoutingKey());
    }

    @Test
    void jsonConverterAndListenerFactoryCanBeBuilt() {
        MessageConverter converter = config.jsonMessageConverter();

        assertNotNull(converter);
        assertNotNull(config.rabbitListenerContainerFactory(
                mock(ConnectionFactory.class),
                converter,
                config.payrollRetryInterceptor(3, 1000, 2.0, 5000),
                true,
                true));
    }

    @Test
    void retryInterceptorCanBeBuiltFromProperties() {
        assertNotNull(config.payrollRetryInterceptor(3, 1000, 2.0, 5000));
    }
}

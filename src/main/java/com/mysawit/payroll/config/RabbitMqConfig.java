package com.mysawit.payroll.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public DirectExchange payrollDeadLetterExchange(
            @Value("${payroll.rabbitmq.dead-letter-exchange:payroll.dlx}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue harvestPayrollQueue(
            @Value("${payroll.rabbitmq.queues.harvest:payroll_queue}") String queueName,
            @Value("${payroll.rabbitmq.dead-letter-exchange:payroll.dlx}") String deadLetterExchange) {
        return payrollQueue(queueName, deadLetterExchange);
    }

    @Bean
    public Queue harvestPayrollDeadLetterQueue(
            @Value("${payroll.rabbitmq.queues.harvest:payroll_queue}") String queueName) {
        return deadLetterQueue(queueName);
    }

    @Bean
    public Binding harvestPayrollDeadLetterBinding(
            Queue harvestPayrollDeadLetterQueue,
            DirectExchange payrollDeadLetterExchange,
            @Value("${payroll.rabbitmq.queues.harvest:payroll_queue}") String queueName) {
        return BindingBuilder.bind(harvestPayrollDeadLetterQueue)
                .to(payrollDeadLetterExchange)
                .with(deadLetterRoutingKey(queueName));
    }

    @Bean
    public Queue shipmentCompletedQueue(
            @Value("${payroll.rabbitmq.queues.shipment:shipment.completed}") String queueName,
            @Value("${payroll.rabbitmq.dead-letter-exchange:payroll.dlx}") String deadLetterExchange) {
        return payrollQueue(queueName, deadLetterExchange);
    }

    @Bean
    public Queue shipmentCompletedDeadLetterQueue(
            @Value("${payroll.rabbitmq.queues.shipment:shipment.completed}") String queueName) {
        return deadLetterQueue(queueName);
    }

    @Bean
    public Binding shipmentCompletedDeadLetterBinding(
            Queue shipmentCompletedDeadLetterQueue,
            DirectExchange payrollDeadLetterExchange,
            @Value("${payroll.rabbitmq.queues.shipment:shipment.completed}") String queueName) {
        return BindingBuilder.bind(shipmentCompletedDeadLetterQueue)
                .to(payrollDeadLetterExchange)
                .with(deadLetterRoutingKey(queueName));
    }

    @Bean
    public Queue userRegisteredQueue(
            @Value("${payroll.rabbitmq.queues.user-registered:user.registered.queue}") String queueName,
            @Value("${payroll.rabbitmq.dead-letter-exchange:payroll.dlx}") String deadLetterExchange) {
        return payrollQueue(queueName, deadLetterExchange);
    }

    @Bean
    public Queue userRegisteredDeadLetterQueue(
            @Value("${payroll.rabbitmq.queues.user-registered:user.registered.queue}") String queueName) {
        return deadLetterQueue(queueName);
    }

    @Bean
    public Binding userRegisteredDeadLetterBinding(
            Queue userRegisteredDeadLetterQueue,
            DirectExchange payrollDeadLetterExchange,
            @Value("${payroll.rabbitmq.queues.user-registered:user.registered.queue}") String queueName) {
        return BindingBuilder.bind(userRegisteredDeadLetterQueue)
                .to(payrollDeadLetterExchange)
                .with(deadLetterRoutingKey(queueName));
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            Advice payrollRetryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup,
            @Value("${spring.rabbitmq.listener.simple.missing-queues-fatal:true}") boolean missingQueuesFatal) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(payrollRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        factory.setMissingQueuesFatal(missingQueuesFatal);
        return factory;
    }

    @Bean
    public Advice payrollRetryInterceptor(
            @Value("${payroll.rabbitmq.retry.max-attempts:3}") int maxAttempts,
            @Value("${payroll.rabbitmq.retry.initial-interval-ms:1000}") long initialIntervalMs,
            @Value("${payroll.rabbitmq.retry.multiplier:2.0}") double multiplier,
            @Value("${payroll.rabbitmq.retry.max-interval-ms:5000}") long maxIntervalMs) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialIntervalMs, multiplier, maxIntervalMs)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    private Queue payrollQueue(String queueName, String deadLetterExchange) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterRoutingKey(queueName))
                .build();
    }

    private Queue deadLetterQueue(String queueName) {
        return QueueBuilder.durable(deadLetterQueueName(queueName)).build();
    }

    private String deadLetterQueueName(String queueName) {
        return queueName + ".dlq";
    }

    private String deadLetterRoutingKey(String queueName) {
        return queueName + ".dead";
    }
}

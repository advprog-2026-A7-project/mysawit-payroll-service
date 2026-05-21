package com.mysawit.payroll.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public Queue harvestPayrollQueue(@Value("${payroll.rabbitmq.queues.harvest:payroll_queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue shipmentCompletedQueue(@Value("${payroll.rabbitmq.queues.shipment:shipment.completed}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue userRegisteredQueue(@Value("${payroll.rabbitmq.queues.user-registered:user.registered.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(payrollRetryInterceptor());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public Advice payrollRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer((message, cause) -> {
                    throw new IllegalStateException("Payroll event failed after retries: " + message, cause);
                })
                .build();
    }
}

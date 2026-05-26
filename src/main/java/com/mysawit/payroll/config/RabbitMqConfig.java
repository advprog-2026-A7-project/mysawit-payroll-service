package com.mysawit.payroll.config;

import com.mysawit.payroll.event.HarvestEvent;
import com.mysawit.payroll.event.ShipmentEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String HARVEST_PAYROLL_QUEUE = "payroll_queue";
    public static final String SHIPMENT_EXCHANGE = "shipment.exchange";
    public static final String SHIPMENT_SUPIR_QUEUE = "payroll.shipment.supir.queue";
    public static final String SHIPMENT_MANDOR_QUEUE = "payroll.shipment.mandor.queue";
    public static final String SHIPMENT_APPROVED_BY_MANDOR_KEY = "shipment.approved-by-mandor";
    public static final String SHIPMENT_APPROVED_BY_ADMIN_KEY = "shipment.approved-by-admin";

    @Bean
    public Queue harvestPayrollQueue() {
        return new Queue(HARVEST_PAYROLL_QUEUE, true);
    }

    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(SHIPMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue shipmentSupirPayrollQueue() {
        return new Queue(SHIPMENT_SUPIR_QUEUE, true);
    }

    @Bean
    public Queue shipmentMandorPayrollQueue() {
        return new Queue(SHIPMENT_MANDOR_QUEUE, true);
    }

    @Bean
    public Binding shipmentSupirPayrollBinding(
            @Qualifier("shipmentSupirPayrollQueue") Queue queue,
            @Qualifier("shipmentExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(SHIPMENT_APPROVED_BY_MANDOR_KEY);
    }

    @Bean
    public Binding shipmentMandorPayrollBinding(
            @Qualifier("shipmentMandorPayrollQueue") Queue queue,
            @Qualifier("shipmentExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(SHIPMENT_APPROVED_BY_ADMIN_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*");

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.mysawit.harvest.event.HarvestPayrollEvent", HarvestEvent.class);
        idClassMapping.put("com.mysawit.shipment.event.ShipmentPayrollEvent", ShipmentEvent.class);
        classMapper.setIdClassMapping(idClassMapping);

        converter.setClassMapper(classMapper);
        return converter;
    }
}

package com.mysawit.payroll.event;

import com.mysawit.payroll.model.UserReplica;
import com.mysawit.payroll.repository.UserReplicaRepository;
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
    private UserReplicaRepository userReplicaRepository;

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
        if (event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("Skipping user.registered event without userId: {}", event);
            return;
        }
        log.info("Received user.registered event for userId={}", event.getUserId());

        UserReplica replica = userReplicaRepository.findById(event.getUserId())
                .orElseGet(UserReplica::new);
        replica.setId(event.getUserId());
        replica.setName(event.getUsername() != null ? event.getUsername() : event.getEmail());
        replica.setRole(event.getRole());
        userReplicaRepository.save(replica);
    }
}

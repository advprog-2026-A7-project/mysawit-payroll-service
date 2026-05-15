package com.mysawit.payroll.event;

import com.mysawit.payroll.model.UserReplica;
import com.mysawit.payroll.repository.UserReplicaRepository;
import com.mysawit.payroll.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollEventConsumerImplTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private PayrollService payrollService;
    private UserReplicaRepository userReplicaRepository;
    private PayrollEventConsumerImpl consumer;

    @BeforeEach
    void setUp() {
        payrollService = mock(PayrollService.class);
        userReplicaRepository = mock(UserReplicaRepository.class);
        consumer = new PayrollEventConsumerImpl();
        ReflectionTestUtils.setField(consumer, "payrollService", payrollService);
        ReflectionTestUtils.setField(consumer, "userReplicaRepository", userReplicaRepository);
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
    void handleUserRegisteredEventCreatesNewReplicaWhenAbsent() {
        UserRegisteredEvent event = new UserRegisteredEvent(USER_ID, "sari@example.com", "BURUH", "sari");
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userReplicaRepository.save(any(UserReplica.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.handleUserRegisteredEvent(event);

        ArgumentCaptor<UserReplica> captor = ArgumentCaptor.forClass(UserReplica.class);
        verify(userReplicaRepository).save(captor.capture());
        UserReplica saved = captor.getValue();
        assertEquals(USER_ID, saved.getId());
        assertEquals("sari", saved.getName());
        assertEquals("BURUH", saved.getRole());
    }

    @Test
    void handleUserRegisteredEventUpdatesExistingReplica() {
        UserReplica existing = new UserReplica(USER_ID, "old-name", "BURUH");
        UserRegisteredEvent event = new UserRegisteredEvent(USER_ID, "sari@example.com", "MANDOR", "sari-new");
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userReplicaRepository.save(any(UserReplica.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.handleUserRegisteredEvent(event);

        ArgumentCaptor<UserReplica> captor = ArgumentCaptor.forClass(UserReplica.class);
        verify(userReplicaRepository).save(captor.capture());
        UserReplica saved = captor.getValue();
        assertSame(existing, saved);
        assertEquals("sari-new", saved.getName());
        assertEquals("MANDOR", saved.getRole());
    }

    @Test
    void handleUserRegisteredEventFallsBackToEmailWhenUsernameMissing() {
        UserRegisteredEvent event = new UserRegisteredEvent(USER_ID, "sari@example.com", "BURUH", null);
        when(userReplicaRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userReplicaRepository.save(any(UserReplica.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.handleUserRegisteredEvent(event);

        ArgumentCaptor<UserReplica> captor = ArgumentCaptor.forClass(UserReplica.class);
        verify(userReplicaRepository).save(captor.capture());
        assertEquals("sari@example.com", captor.getValue().getName());
    }

    @Test
    void handleUserRegisteredEventSkipsWhenUserIdMissing() {
        UserRegisteredEvent event = new UserRegisteredEvent(null, "x@example.com", "BURUH", "x");

        consumer.handleUserRegisteredEvent(event);

        verifyNoInteractions(userReplicaRepository);
    }

    @Test
    void handleUserRegisteredEventSkipsWhenUserIdBlank() {
        UserRegisteredEvent event = new UserRegisteredEvent("   ", "x@example.com", "BURUH", "x");

        consumer.handleUserRegisteredEvent(event);

        verifyNoInteractions(userReplicaRepository);
    }
}

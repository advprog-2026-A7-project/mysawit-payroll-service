package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.PayrollBrokerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollBrokerEventRepository extends JpaRepository<PayrollBrokerEvent, Long> {
    boolean existsByEventKey(String eventKey);
}

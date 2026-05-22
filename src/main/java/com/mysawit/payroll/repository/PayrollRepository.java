package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByUserId(String userId);
    List<Payroll> findByStatus(String status);
    List<Payroll> findByPeriodStartBetween(LocalDateTime start, LocalDateTime end);
    List<Payroll> findByUserIdAndStatus(String userId, String status);
    List<Payroll> findByUserIdAndPeriodStartBetween(String userId, LocalDateTime start, LocalDateTime end);
    List<Payroll> findByUserIdAndStatusAndPeriodStartBetween(
            String userId,
            String status,
            LocalDateTime start,
            LocalDateTime end);
}

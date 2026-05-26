package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployeeId(String employeeId);
    List<Payroll> findByUserId(String userId);
    List<Payroll> findByStatus(String status);
    List<Payroll> findByPeriodStartBetween(LocalDateTime start, LocalDateTime end);
    List<Payroll> findByEmployeeIdAndStatus(String employeeId, String status);
    List<Payroll> findByUserIdAndStatus(String userId, String status);

    Payroll findByEventId(String eventId);

    @Query("""
            SELECT p FROM Payroll p
            WHERE (:userId IS NULL OR p.userId = :userId)
              AND (:status IS NULL OR p.status = :status)
              AND (:from IS NULL OR p.periodStart >= :from)
              AND (:to IS NULL OR p.periodStart <= :to)
            ORDER BY p.createdAt DESC
            """)
    List<Payroll> search(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}

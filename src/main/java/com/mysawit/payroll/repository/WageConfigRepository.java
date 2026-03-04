package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.WageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WageConfigRepository extends JpaRepository<WageConfig, Long> {

    List<WageConfig> findByRoleType(String roleType);

    List<WageConfig> findByRoleTypeOrderByEffectiveDateDesc(String roleType);

    /**
     * Get the latest active wage config for a role (most recent effectiveDate <= today).
     */
    @Query("SELECT w FROM WageConfig w WHERE w.roleType = :roleType AND w.effectiveDate <= :today ORDER BY w.effectiveDate DESC")
    List<WageConfig> findActiveConfigForRole(@Param("roleType") String roleType, @Param("today") LocalDate today);

    Optional<WageConfig> findFirstByRoleTypeOrderByEffectiveDateDesc(String roleType);
}

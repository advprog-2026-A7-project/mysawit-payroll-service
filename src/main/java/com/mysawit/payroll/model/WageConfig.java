package com.mysawit.payroll.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "wage_configs")
public class WageConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role type for this wage config.
     * Allowed values: BURUH (worker), SUPIR (driver), MANDOR (foreman)
     */
    @Column(name = "role_type", nullable = false)
    private String roleType;

    /**
     * Upah (wage) per kilogram harvested, in IDR.
     */
    @Column(name = "rate_per_kg", nullable = false)
    private Double ratePerKg;

    /**
     * Date from which this config is effective.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by")
    private String createdBy;

    // Constructors
    public WageConfig() {}

    public WageConfig(String roleType, Double ratePerKg, LocalDate effectiveDate) {
        this.roleType = roleType;
        this.ratePerKg = ratePerKg;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public Double getRatePerKg() { return ratePerKg; }
    public void setRatePerKg(Double ratePerKg) { this.ratePerKg = ratePerKg; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

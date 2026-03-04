package com.mysawit.payroll.service;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class WageConfigService {

    @Autowired
    private WageConfigRepository wageConfigRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<WageConfig> getAllConfigs() {
        return wageConfigRepository.findAll();
    }

    public Optional<WageConfig> getConfigById(Long id) {
        return wageConfigRepository.findById(id);
    }

    public List<WageConfig> getConfigsByRole(String roleType) {
        return wageConfigRepository.findByRoleTypeOrderByEffectiveDateDesc(roleType.toUpperCase());
    }

    /**
     * Returns the currently active (latest effective) config for the given role.
     */
    public Optional<WageConfig> getActiveConfigForRole(String roleType) {
        List<WageConfig> configs = wageConfigRepository
                .findActiveConfigForRole(roleType.toUpperCase(), LocalDate.now());
        return configs.isEmpty() ? Optional.empty() : Optional.of(configs.get(0));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public WageConfig createConfig(WageConfig wageConfig) {
        validateRoleType(wageConfig.getRoleType());
        wageConfig.setRoleType(wageConfig.getRoleType().toUpperCase());
        return wageConfigRepository.save(wageConfig);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public WageConfig updateConfig(Long id, WageConfig details) {
        WageConfig existing = wageConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WageConfig not found with id: " + id));

        if (details.getRoleType() != null) {
            validateRoleType(details.getRoleType());
            existing.setRoleType(details.getRoleType().toUpperCase());
        }
        if (details.getRatePerKg() != null) {
            existing.setRatePerKg(details.getRatePerKg());
        }
        if (details.getEffectiveDate() != null) {
            existing.setEffectiveDate(details.getEffectiveDate());
        }
        if (details.getDescription() != null) {
            existing.setDescription(details.getDescription());
        }
        if (details.getCreatedBy() != null) {
            existing.setCreatedBy(details.getCreatedBy());
        }

        return wageConfigRepository.save(existing);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteConfig(Long id) {
        WageConfig existing = wageConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WageConfig not found with id: " + id));
        wageConfigRepository.delete(existing);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateRoleType(String roleType) {
        String upper = roleType.toUpperCase();
        if (!upper.equals("BURUH") && !upper.equals("SUPIR") && !upper.equals("MANDOR")) {
            throw new IllegalArgumentException(
                    "Invalid roleType: " + roleType + ". Allowed values: BURUH, SUPIR, MANDOR");
        }
    }
}

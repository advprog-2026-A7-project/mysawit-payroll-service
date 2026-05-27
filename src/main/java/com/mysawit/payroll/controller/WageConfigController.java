package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.service.WageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing wage-per-kg configuration.
 *
 * Base path : /api/admin/wage-configs
 *
 * GET    /api/admin/wage-configs              – list all configs
 * GET    /api/admin/wage-configs/{id}         – get config by id
 * GET    /api/admin/wage-configs/role/{role}  – list configs for a role (BURUH/SUPIR/MANDOR)
 * GET    /api/admin/wage-configs/role/{role}/active – currently active config for role
 * POST   /api/admin/wage-configs              – create config
 * PUT    /api/admin/wage-configs/{id}         – replace config
 * PATCH  /api/admin/wage-configs/{id}         – partial update
 * DELETE /api/admin/wage-configs/{id}         – delete config
 */
@RestController
@RequestMapping("/api/admin/wage-configs")
public class WageConfigController {

    @Autowired
    private WageConfigService wageConfigService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<WageConfig>> getAllConfigs() {
        return ResponseEntity.ok(wageConfigService.getAllConfigs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WageConfig> getConfigById(@PathVariable Long id) {
        return wageConfigService.getConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<WageConfig>> getConfigsByRole(@PathVariable String role) {
        return ResponseEntity.ok(wageConfigService.getConfigsByRole(role));
    }

    @GetMapping("/role/{role}/active")
    public ResponseEntity<WageConfig> getActiveConfigForRole(@PathVariable String role) {
        return wageConfigService.getActiveConfigForRole(role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody WageConfig wageConfig) {
        try {
            WageConfig created = wageConfigService.createConfig(wageConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateConfig(@PathVariable Long id, @RequestBody WageConfig wageConfig) {
        try {
            WageConfig updated = wageConfigService.updateConfig(id, wageConfig);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        try {
            wageConfigService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

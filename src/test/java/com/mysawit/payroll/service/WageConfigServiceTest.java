package com.mysawit.payroll.service;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WageConfigServiceTest {

    @Mock
    private WageConfigRepository wageConfigRepository;

    @InjectMocks
    private WageConfigService wageConfigService;

    private WageConfig buruh;

    @BeforeEach
    void setUp() {
        buruh = new WageConfig("BURUH", 350.0, LocalDate.of(2026, 1, 1));
        buruh.setId(1L);
        buruh.setDescription("Upah panen buruh");
        buruh.setCreatedBy("admin");
    }

    // ── getAllConfigs ─────────────────────────────────────────────────────────

    @Test
    void getAllConfigsReturnsAll() {
        when(wageConfigRepository.findAll()).thenReturn(List.of(buruh));
        List<WageConfig> result = wageConfigService.getAllConfigs();
        assertEquals(1, result.size());
        verify(wageConfigRepository).findAll();
    }

    // ── getConfigById ─────────────────────────────────────────────────────────

    @Test
    void getConfigByIdFoundReturnsOptional() {
        when(wageConfigRepository.findById(1L)).thenReturn(Optional.of(buruh));
        Optional<WageConfig> result = wageConfigService.getConfigById(1L);
        assertTrue(result.isPresent());
        assertEquals("BURUH", result.get().getRoleType());
    }

    @Test
    void getConfigByIdNotFoundReturnsEmpty() {
        when(wageConfigRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<WageConfig> result = wageConfigService.getConfigById(99L);
        assertTrue(result.isEmpty());
    }

    // ── getConfigsByRole ──────────────────────────────────────────────────────

    @Test
    void getConfigsByRoleReturnsList() {
        when(wageConfigRepository.findByRoleTypeOrderByEffectiveDateDesc("BURUH"))
                .thenReturn(List.of(buruh));
        List<WageConfig> result = wageConfigService.getConfigsByRole("BURUH");
        assertEquals(1, result.size());
    }

    @Test
    void getConfigsByRoleUppercasesInput() {
        when(wageConfigRepository.findByRoleTypeOrderByEffectiveDateDesc("SUPIR"))
                .thenReturn(List.of());
        wageConfigService.getConfigsByRole("supir");
        verify(wageConfigRepository).findByRoleTypeOrderByEffectiveDateDesc("SUPIR");
    }

    // ── getActiveConfigForRole ────────────────────────────────────────────────

    @Test
    void getActiveConfigForRoleFoundReturnsFirst() {
        when(wageConfigRepository.findActiveConfigForRole(eq("BURUH"), any(LocalDate.class)))
                .thenReturn(List.of(buruh));
        Optional<WageConfig> result = wageConfigService.getActiveConfigForRole("BURUH");
        assertTrue(result.isPresent());
    }

    @Test
    void getActiveConfigForRoleNotFoundReturnsEmpty() {
        when(wageConfigRepository.findActiveConfigForRole(eq("MANDOR"), any(LocalDate.class)))
                .thenReturn(List.of());
        Optional<WageConfig> result = wageConfigService.getActiveConfigForRole("MANDOR");
        assertTrue(result.isEmpty());
    }

    // ── createConfig ──────────────────────────────────────────────────────────

    @Test
    void createConfigSavesAndReturns() {
        when(wageConfigRepository.save(any())).thenReturn(buruh);
        WageConfig result = wageConfigService.createConfig(new WageConfig("BURUH", 350.0, LocalDate.now()));
        assertEquals("BURUH", result.getRoleType());
        verify(wageConfigRepository).save(any());
    }

    @Test
    void createConfigUppercasesRoleType() {
        WageConfig input = new WageConfig("buruh", 350.0, LocalDate.now());
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WageConfig result = wageConfigService.createConfig(input);
        assertEquals("BURUH", result.getRoleType());
    }

    @Test
    void createConfigThrowsForInvalidRole() {
        WageConfig bad = new WageConfig("INVALID", 100.0, LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> wageConfigService.createConfig(bad));
    }

    // ── updateConfig ──────────────────────────────────────────────────────────

    @Test
    void updateConfigUpdatesAllFields() {
        WageConfig updates = new WageConfig("SUPIR", 300.0, LocalDate.of(2026, 3, 1));
        updates.setDescription("Updated");
        updates.setCreatedBy("admin2");

        when(wageConfigRepository.findById(1L)).thenReturn(Optional.of(buruh));
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WageConfig result = wageConfigService.updateConfig(1L, updates);
        assertEquals("SUPIR", result.getRoleType());
        assertEquals(300.0, result.getRatePerKg());
        assertEquals("Updated", result.getDescription());
        assertEquals("admin2", result.getCreatedBy());
    }

    @Test
    void updateConfigWithNullFieldsSkipsUpdate() {
        WageConfig updates = new WageConfig();
        when(wageConfigRepository.findById(1L)).thenReturn(Optional.of(buruh));
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WageConfig result = wageConfigService.updateConfig(1L, updates);
        assertEquals("BURUH", result.getRoleType());
        assertEquals(350.0, result.getRatePerKg());
    }

    @Test
    void updateConfigThrowsWhenNotFound() {
        when(wageConfigRepository.findById(99L)).thenReturn(Optional.empty());
        WageConfig updates = new WageConfig("BURUH", 300.0, LocalDate.now());
        assertThrows(RuntimeException.class, () -> wageConfigService.updateConfig(99L, updates));
    }

    @Test
    void updateConfigThrowsForInvalidRoleType() {
        WageConfig updates = new WageConfig("INVALID", 100.0, LocalDate.now());
        when(wageConfigRepository.findById(1L)).thenReturn(Optional.of(buruh));
        assertThrows(IllegalArgumentException.class, () -> wageConfigService.updateConfig(1L, updates));
    }

    // ── deleteConfig ──────────────────────────────────────────────────────────

    @Test
    void deleteConfigDeletesSuccessfully() {
        when(wageConfigRepository.findById(1L)).thenReturn(Optional.of(buruh));
        wageConfigService.deleteConfig(1L);
        verify(wageConfigRepository).delete(buruh);
    }

    @Test
    void deleteConfigThrowsWhenNotFound() {
        when(wageConfigRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> wageConfigService.deleteConfig(99L));
    }
}

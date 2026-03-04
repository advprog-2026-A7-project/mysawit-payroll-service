package com.mysawit.payroll.controller;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.service.WageConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WageConfigControllerTest {

    @Mock
    private WageConfigService wageConfigService;

    @InjectMocks
    private WageConfigController wageConfigController;

    private WageConfig buruh;

    @BeforeEach
    void setUp() {
        buruh = new WageConfig("BURUH", 350.0, LocalDate.of(2026, 1, 1));
        buruh.setId(1L);
    }

    // ── GET all ───────────────────────────────────────────────────────────────

    @Test
    void getAllConfigsReturns200WithList() {
        when(wageConfigService.getAllConfigs()).thenReturn(List.of(buruh));
        ResponseEntity<List<WageConfig>> response = wageConfigController.getAllConfigs();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @Test
    void getConfigByIdFoundReturns200() {
        when(wageConfigService.getConfigById(1L)).thenReturn(Optional.of(buruh));
        ResponseEntity<WageConfig> response = wageConfigController.getConfigById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getConfigByIdNotFoundReturns404() {
        when(wageConfigService.getConfigById(99L)).thenReturn(Optional.empty());
        ResponseEntity<WageConfig> response = wageConfigController.getConfigById(99L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── GET by role ───────────────────────────────────────────────────────────

    @Test
    void getConfigsByRoleReturns200WithList() {
        when(wageConfigService.getConfigsByRole("BURUH")).thenReturn(List.of(buruh));
        ResponseEntity<List<WageConfig>> response = wageConfigController.getConfigsByRole("BURUH");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── GET active by role ────────────────────────────────────────────────────

    @Test
    void getActiveConfigForRoleFoundReturns200() {
        when(wageConfigService.getActiveConfigForRole("BURUH")).thenReturn(Optional.of(buruh));
        ResponseEntity<WageConfig> response = wageConfigController.getActiveConfigForRole("BURUH");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getActiveConfigForRoleNotFoundReturns404() {
        when(wageConfigService.getActiveConfigForRole("MANDOR")).thenReturn(Optional.empty());
        ResponseEntity<WageConfig> response = wageConfigController.getActiveConfigForRole("MANDOR");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── POST create ───────────────────────────────────────────────────────────

    @Test
    void createConfigReturns201OnSuccess() {
        when(wageConfigService.createConfig(any())).thenReturn(buruh);
        ResponseEntity<?> response = wageConfigController.createConfig(buruh);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createConfigReturns400OnInvalidRole() {
        when(wageConfigService.createConfig(any()))
                .thenThrow(new IllegalArgumentException("Invalid roleType"));
        ResponseEntity<?> response = wageConfigController.createConfig(new WageConfig("INVALID", 100.0, LocalDate.now()));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    void updateConfigReturns200OnSuccess() {
        when(wageConfigService.updateConfig(eq(1L), any())).thenReturn(buruh);
        ResponseEntity<?> response = wageConfigController.updateConfig(1L, buruh);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateConfigReturns404WhenNotFound() {
        when(wageConfigService.updateConfig(eq(99L), any()))
                .thenThrow(new RuntimeException("WageConfig not found with id: 99"));
        ResponseEntity<?> response = wageConfigController.updateConfig(99L, buruh);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateConfigReturns400OnInvalidRole() {
        when(wageConfigService.updateConfig(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Invalid roleType"));
        ResponseEntity<?> response = wageConfigController.updateConfig(1L, buruh);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── PATCH partial update ──────────────────────────────────────────────────

    @Test
    void patchConfigReturns200OnSuccess() {
        when(wageConfigService.updateConfig(eq(1L), any())).thenReturn(buruh);
        ResponseEntity<?> response = wageConfigController.patchConfig(1L, buruh);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void patchConfigReturns404WhenNotFound() {
        when(wageConfigService.updateConfig(eq(99L), any()))
                .thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> response = wageConfigController.patchConfig(99L, buruh);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void patchConfigReturns400OnInvalidRole() {
        when(wageConfigService.updateConfig(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Invalid roleType"));
        ResponseEntity<?> response = wageConfigController.patchConfig(1L, buruh);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteConfigReturns204OnSuccess() {
        doNothing().when(wageConfigService).deleteConfig(1L);
        ResponseEntity<Void> response = wageConfigController.deleteConfig(1L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteConfigReturns404WhenNotFound() {
        doThrow(new RuntimeException("not found")).when(wageConfigService).deleteConfig(99L);
        ResponseEntity<Void> response = wageConfigController.deleteConfig(99L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

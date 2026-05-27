package com.mysawit.payroll.config;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private WageConfigRepository wageConfigRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void runSeedsThreeWageConfigsWhenNoneExist() throws Exception {
        when(wageConfigRepository.count()).thenReturn(0L);
        when(wageConfigRepository.save(any(WageConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        verify(wageConfigRepository, times(3)).save(any(WageConfig.class));
    }

    @Test
    void runSkipsSeedingWhenWageConfigsAlreadyExist() throws Exception {
        when(wageConfigRepository.count()).thenReturn(3L);

        dataInitializer.run();

        verify(wageConfigRepository, never()).save(any(WageConfig.class));
    }
}

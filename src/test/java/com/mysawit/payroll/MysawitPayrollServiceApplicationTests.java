package com.mysawit.payroll;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.mockito.Mockito.*;

class MysawitPayrollServiceApplicationTests {

    @Test
    void constructorCanBeCalled() {
        new MysawitPayrollServiceApplication();
    }

    @Test
    void mainClosesContextWhenConfigured() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("app.test.close-context", Boolean.class, false)).thenReturn(true);

        String[] args = new String[] {"--spring.main.web-application-type=none"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(MysawitPayrollServiceApplication.class, args)).thenReturn(context);

            MysawitPayrollServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(MysawitPayrollServiceApplication.class, args));
            verify(context).close();
        }
    }

    @Test
    void mainDoesNotCloseContextWhenNotConfigured() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("app.test.close-context", Boolean.class, false)).thenReturn(false);

        String[] args = new String[] {"--spring.main.web-application-type=none"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(MysawitPayrollServiceApplication.class, args)).thenReturn(context);

            MysawitPayrollServiceApplication.main(args);

            verify(context, never()).close();
        }
    }
}

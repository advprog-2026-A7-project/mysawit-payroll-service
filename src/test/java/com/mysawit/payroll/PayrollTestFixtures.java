package com.mysawit.payroll;

import com.mysawit.payroll.model.Payroll;

import java.time.LocalDateTime;

public class PayrollTestFixtures {

    public static final String SAMPLE_USER_ID = "11111111-1111-1111-1111-111111111111";

    public static Payroll pendingPayroll() {
        Payroll p = new Payroll();
        p.setId(1L);
        p.setUserId(SAMPLE_USER_ID);
        p.setStatus("PENDING");
        p.setBaseAmount(5000000.0);
        p.setBonusAmount(500000.0);
        p.setDeductionAmount(250000.0);
        p.setTotalAmount(5250000.0);
        p.setPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        p.setPeriodEnd(LocalDateTime.of(2026, 1, 31, 23, 59));
        return p;
    }
}

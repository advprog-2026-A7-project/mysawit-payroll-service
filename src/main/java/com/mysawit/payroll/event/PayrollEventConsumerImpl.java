package com.mysawit.payroll.event;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.mysawit.payroll.service.PayrollService;

@Service
public class PayrollEventConsumerImpl implements PayrollEventConsumer {
    @Autowired
    private PayrollService payrollService;

    @Override
    public void handleHarvestEvent(HarvestEvent event) {
        payrollService.processHarvestPayroll(event);
    }

    @Override
    public void handleShipmentEvent(ShipmentEvent event) {
        payrollService.processShipmentPayroll(event);
    }
}

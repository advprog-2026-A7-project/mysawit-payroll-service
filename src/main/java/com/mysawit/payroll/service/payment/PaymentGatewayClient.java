package com.mysawit.payroll.service.payment;

public interface PaymentGatewayClient {
    PaymentGatewayInvoice createTopUpInvoice(String externalId, String userId, double amountSawitDollar, double amountIdr);
}

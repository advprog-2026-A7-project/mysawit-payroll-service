package com.mysawit.payroll.service.payment;

public record PaymentGatewayInvoice(
        String gatewayTransactionId,
        String status,
        String checkoutUrl
) {
}

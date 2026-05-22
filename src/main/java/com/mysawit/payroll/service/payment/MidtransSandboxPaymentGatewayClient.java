package com.mysawit.payroll.service.payment;

import com.mysawit.payroll.config.MidtransSandboxProperties;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MidtransSandboxPaymentGatewayClient implements PaymentGatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final MidtransSandboxProperties properties;

    public MidtransSandboxPaymentGatewayClient(
            RestClient.Builder restClientBuilder,
            MidtransSandboxProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    @Override
    public PaymentGatewayInvoice createTopUpInvoice(
            String externalId,
            String userId,
            double amountSawitDollar,
            double amountIdr) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Midtrans sandbox integration is disabled");
        }
        if (properties.getServerKey() == null || properties.getServerKey().isBlank()) {
            throw new IllegalStateException("MIDTRANS_SERVER_KEY is required to create a Midtrans sandbox transaction");
        }

        long grossAmount = Math.round(amountIdr);
        Map<String, Object> payload = Map.of(
                "transaction_details", Map.of(
                        "order_id", externalId,
                        "gross_amount", grossAmount),
                "item_details", List.of(Map.of(
                        "id", "sawit-dollar-topup",
                        "price", grossAmount,
                        "quantity", 1,
                        "name", "SawitDollar wallet top-up")),
                "customer_details", Map.of(
                        "first_name", userId),
                "callbacks", Map.of(
                        "finish", properties.getFinishRedirectUrl()),
                "expiry", Map.of(
                        "unit", "minutes",
                        "duration", properties.getExpiryDurationMinutes()),
                "custom_field1", userId,
                "custom_field2", String.valueOf(amountSawitDollar),
                "custom_field3", "mysawit-payroll-service");

        Map<String, Object> response = restClientBuilder
                .baseUrl(properties.getSnapBaseUrl())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.getServerKey(), "");
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .build()
                .post()
                .uri("/snap/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException("Midtrans did not return a Snap transaction response");
        }

        String redirectUrl = stringValue(response.get("redirect_url"));
        if (redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalStateException("Midtrans Snap response did not include redirect_url");
        }

        return new PaymentGatewayInvoice(
                firstText(response.get("token"), externalId),
                "PENDING",
                redirectUrl);
    }

    private String firstText(Object value, String fallback) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? fallback : text;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}

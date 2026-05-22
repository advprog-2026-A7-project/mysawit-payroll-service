package com.mysawit.payroll.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "midtrans.sandbox")
public class MidtransSandboxProperties {

    private boolean enabled = true;
    private String snapBaseUrl = "https://app.sandbox.midtrans.com";
    private String merchantId;
    private String clientKey;
    private String serverKey;
    private String finishRedirectUrl = "http://localhost:3000/admin/payroll?topUp=finish";
    private int expiryDurationMinutes = 1440;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSnapBaseUrl() {
        return snapBaseUrl;
    }

    public void setSnapBaseUrl(String snapBaseUrl) {
        this.snapBaseUrl = snapBaseUrl;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

    public String getFinishRedirectUrl() {
        return finishRedirectUrl;
    }

    public void setFinishRedirectUrl(String finishRedirectUrl) {
        this.finishRedirectUrl = finishRedirectUrl;
    }

    public int getExpiryDurationMinutes() {
        return expiryDurationMinutes;
    }

    public void setExpiryDurationMinutes(int expiryDurationMinutes) {
        this.expiryDurationMinutes = expiryDurationMinutes;
    }
}

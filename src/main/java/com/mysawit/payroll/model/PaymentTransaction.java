package com.mysawit.payroll.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String gateway;

    @Column(nullable = false)
    private String status;

    @Column(name = "amount_sawit_dollar", nullable = false)
    private Double amountSawitDollar;

    @Column(name = "amount_idr", nullable = false)
    private Double amountIdr;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAmountSawitDollar() {
        return amountSawitDollar;
    }

    public void setAmountSawitDollar(Double amountSawitDollar) {
        this.amountSawitDollar = amountSawitDollar;
    }

    public Double getAmountIdr() {
        return amountIdr;
    }

    public void setAmountIdr(Double amountIdr) {
        this.amountIdr = amountIdr;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}

package com.mysawit.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.payroll.repository.PaymentTransactionRepository;
import com.mysawit.payroll.repository.PayrollRepository;
import com.mysawit.payroll.repository.WageConfigRepository;
import com.mysawit.payroll.repository.WalletRepository;
import com.mysawit.payroll.service.payment.PaymentGatewayClient;
import com.mysawit.payroll.service.payment.PaymentGatewayInvoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PayrollIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private WageConfigRepository wageConfigRepository;

    @MockitoBean
    private PaymentGatewayClient paymentGatewayClient;

    @BeforeEach
    void cleanDatabase() {
        paymentTransactionRepository.deleteAll();
        payrollRepository.deleteAll();
        walletRepository.deleteAll();
        wageConfigRepository.deleteAll();
    }

    @Test
    void payrollWalletAndWageFlowWorksEndToEnd() throws Exception {
        String config = mockMvc.perform(post("/api/admin/wage-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleType": "buruh",
                                  "ratePerKg": 350,
                                  "effectiveDate": "2026-01-01",
                                  "description": "Harvest wage",
                                  "createdBy": "admin"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleType").value("BURUH"))
                .andExpect(jsonPath("$.ratePerKg").value(350.0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long configId = objectMapper.readTree(config).get("id").asLong();

        mockMvc.perform(get("/api/admin/wage-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/admin/wage-configs/{id}", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId));
        mockMvc.perform(get("/api/admin/wage-configs/role/{role}", "buruh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/admin/wage-configs/role/{role}/active", "BURUH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId));

        when(paymentGatewayClient.createTopUpInvoice(anyString(), anyString(), anyDouble(), anyDouble()))
                .thenAnswer(inv -> new PaymentGatewayInvoice(
                        "midtrans-token-test",
                        "PENDING",
                        "https://app.sandbox.midtrans.com/snap/v4/redirection/token-test"));

        String transaction = mockMvc.perform(post("/api/wallets/{userId}/top-up/sandbox", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountSawitDollar": "1000",
                                  "gateway": "MIDTRANS_SANDBOX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amountSawitDollar").value(1000.0))
                .andExpect(jsonPath("$.amountIdr").value(10000000.0))
                .andExpect(jsonPath("$.checkoutUrl").value("https://app.sandbox.midtrans.com/snap/v4/redirection/token-test"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String transactionId = objectMapper.readTree(transaction).get("transactionId").asText();

        mockMvc.perform(post("/api/wallets/transactions/{transactionId}/settle-sandbox", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        String payroll = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "worker-1",
                                  "roleType": "BURUH",
                                  "periodStart": "2026-05-01T00:00:00",
                                  "periodEnd": "2026-05-31T23:59:00",
                                  "baseAmount": 100,
                                  "bonusAmount": 20,
                                  "deductionAmount": 5,
                                  "status": "PENDING",
                                  "notes": "Manual payroll"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("worker-1"))
                .andExpect(jsonPath("$.totalAmount").value(115.0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long payrollId = objectMapper.readTree(payroll).get("id").asLong();

        mockMvc.perform(get("/api/payrolls").param("userId", "worker-1").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/payrolls")
                        .param("userId", "worker-1")
                        .param("status", "PENDING")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-06-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/payrolls/user/{userId}", "worker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/payrolls/status/{status}", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/payrolls/{id}/approve", payrollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adminId": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.walletSettled").value(true))
                .andExpect(jsonPath("$.walletTransferAmount").value(115.0));

        mockMvc.perform(get("/api/wallets/{userId}", "worker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(115.0));
        mockMvc.perform(get("/api/wallets/{userId}", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(885.0));
        mockMvc.perform(get("/api/wallets/{userId}/transactions", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/payrolls/{id}/pay", payrollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "SANDBOX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("SANDBOX"));

        mockMvc.perform(get("/api/payrolls/{id}", payrollId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payrollId));

        mockMvc.perform(put("/api/admin/wage-configs/{id}", configId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ratePerKg": 400,
                                  "description": "Updated wage",
                                  "createdBy": "owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerKg").value(400.0))
                .andExpect(jsonPath("$.description").value("Updated wage"))
                .andExpect(jsonPath("$.createdBy").value("owner"));

        mockMvc.perform(delete("/api/admin/wage-configs/{id}", configId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/wage-configs/{id}", configId))
                .andExpect(status().isNotFound());
    }

    @Test
    void payrollEventRejectAndErrorPathsAreExercised() throws Exception {
        mockMvc.perform(post("/api/admin/wage-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleType": "BURUH",
                                  "ratePerKg": 350,
                                  "effectiveDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payrolls/demo/harvest-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "harvest-1",
                                  "employeeId": "worker-2",
                                  "kilograms": 100,
                                  "timestamp": 1779400000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("harvest-1"));

        mockMvc.perform(post("/api/payrolls/demo/harvest-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "",
                                  "employeeId": "worker-2",
                                  "amount": 100
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").isNotEmpty());

        String rejectedPayroll = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "worker-3",
                                  "roleType": "BURUH",
                                  "periodStart": "2026-05-01T00:00:00",
                                  "periodEnd": "2026-05-31T23:59:00",
                                  "baseAmount": 50,
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long rejectedPayrollId = objectMapper.readTree(rejectedPayroll).get("id").asLong();

        mockMvc.perform(patch("/api/payrolls/{id}/reject", rejectedPayrollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Invalid attendance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Invalid attendance"));

        mockMvc.perform(patch("/api/payrolls/{id}/accept", rejectedPayrollId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(post("/api/wallets/{userId}/top-up/sandbox", "worker-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountSawitDollar": "0"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Top-up amount must be greater than zero"));

        mockMvc.perform(post("/api/admin/wage-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleType": "INVALID",
                                  "ratePerKg": 1,
                                  "effectiveDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(put("/api/admin/wage-configs/{id}", 404)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleType": "BURUH"
                                }
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/wage-configs/{id}", 404))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/wage-configs/role/{role}/active", "SUPIR"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/payrolls/{id}", 404))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/payrolls/{id}", 404)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "worker-404",
                                  "periodStart": "2026-05-01T00:00:00",
                                  "periodEnd": "2026-05-31T23:59:00",
                                  "baseAmount": 10,
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/payrolls/{id}", 404))
                .andExpect(status().isNotFound());
    }
}

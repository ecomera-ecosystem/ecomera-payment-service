package com.ecomera.payment.payment.controller;

import com.ecomera.payment.payment.dto.PaymentCreateRequest;
import com.ecomera.payment.payment.dto.PaymentDto;
import com.ecomera.payment.payment.dto.PaymentRefundRequest;
import com.ecomera.payment.payment.dto.PaymentUpdateRequest;
import com.ecomera.payment.payment.enums.PaymentMethod;
import com.ecomera.payment.payment.enums.PaymentStatus;
import com.ecomera.payment.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String rolesHeader = "USER,ADMIN";

    @Test
    void create_shouldReturn201() throws Exception {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .orderId(orderId)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        PaymentDto paymentDto = PaymentDto.builder()
                .id(paymentId).orderId(orderId).userId(userId).email(email)
                .amount(BigDecimal.valueOf(99.99)).currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING)
                .build();
        given(paymentService.createPayment(any(UUID.class), anyString(), any(PaymentCreateRequest.class)))
                .willReturn(paymentDto);

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    void create_shouldReturn400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {
        Page<PaymentDto> page = new PageImpl<>(List.of());
        given(paymentService.getAll(any(PageRequest.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/payments")
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        PaymentDto paymentDto = PaymentDto.builder()
                .id(paymentId).orderId(orderId).userId(userId).email(email)
                .amount(BigDecimal.valueOf(99.99)).currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING)
                .build();
        given(paymentService.getById(paymentId)).willReturn(paymentDto);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    void getByOrderId_shouldReturn200() throws Exception {
        PaymentDto paymentDto = PaymentDto.builder()
                .id(paymentId).orderId(orderId).userId(userId).email(email)
                .amount(BigDecimal.valueOf(99.99)).currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING)
                .build();
        given(paymentService.getByOrderId(orderId)).willReturn(paymentDto);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void update_shouldReturn200_whenAdmin() throws Exception {
        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .status(PaymentStatus.SUCCEEDED).build();
        PaymentDto paymentDto = PaymentDto.builder()
                .id(paymentId).orderId(orderId).userId(userId).email(email)
                .amount(BigDecimal.valueOf(99.99)).currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD).status(PaymentStatus.SUCCEEDED)
                .build();
        given(paymentService.updatePaymentStatus(any(UUID.class), any(PaymentUpdateRequest.class)))
                .willReturn(paymentDto);

        mockMvc.perform(patch("/api/v1/payments/{id}", paymentId)
                        .header("X-User-Roles", rolesHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Succeeded"));
    }

    @Test
    void update_shouldReturn403_whenNotAdmin() throws Exception {
        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .status(PaymentStatus.SUCCEEDED).build();

        mockMvc.perform(patch("/api/v1/payments/{id}", paymentId)
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturn204_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/{id}", paymentId)
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/{id}", paymentId)
                        .header("X-User-Roles", "MANAGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void handleWebhook_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "test_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refund_shouldReturn200_whenAdmin() throws Exception {
        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .amount(BigDecimal.valueOf(50.00)).build();
        PaymentDto paymentDto = PaymentDto.builder()
                .id(paymentId).orderId(orderId).userId(userId).email(email)
                .amount(BigDecimal.valueOf(99.99)).currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD).status(PaymentStatus.PARTIALLY_REFUNDED)
                .build();
        given(paymentService.refundPayment(any(UUID.class), any(PaymentRefundRequest.class)))
                .willReturn(paymentDto);

        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .header("X-User-Roles", rolesHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void refund_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }
}

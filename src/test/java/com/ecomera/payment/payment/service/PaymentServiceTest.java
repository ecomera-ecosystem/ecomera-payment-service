package com.ecomera.payment.payment.service;

import com.ecomera.payment.client.OrderServiceClient;
import com.ecomera.payment.client.dto.OrderDto;
import com.ecomera.payment.client.dto.OrderStatusUpdateDto;
import com.ecomera.payment.payment.dto.PaymentCreateRequest;
import com.ecomera.payment.payment.dto.PaymentDto;
import com.ecomera.payment.payment.dto.PaymentRefundRequest;
import com.ecomera.payment.payment.dto.PaymentUpdateRequest;
import com.ecomera.payment.payment.entity.Payment;
import com.ecomera.payment.payment.enums.PaymentMethod;
import com.ecomera.payment.payment.enums.PaymentStatus;
import com.ecomera.payment.payment.mapper.PaymentMapper;
import com.ecomera.payment.payment.repository.PaymentRepository;
import com.ecomera.payment.shared.common.exception.BusinessException;
import com.ecomera.payment.shared.common.exception.ResourceNotFoundException;
import com.ecomera.payment.shared.kafka.NotificationEventProducer;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private NotificationEventProducer notificationProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private UUID userId;
    private UUID paymentId;
    private UUID orderId;
    private String email;
    private Payment payment;
    private PaymentDto paymentDto;
    private PaymentCreateRequest createRequest;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        email = "test@example.com";

        createRequest = PaymentCreateRequest.builder()
                .orderId(orderId)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .email(email)
                .amount(BigDecimal.valueOf(99.99))
                .currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_test_123")
                .build();

        paymentDto = PaymentDto.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .email(email)
                .amount(BigDecimal.valueOf(99.99))
                .currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        orderDto = OrderDto.builder()
                .id(orderId)
                .totalPrice(BigDecimal.valueOf(99.99))
                .build();

        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "MAD");
    }

    @Test
    void createPayment_shouldCreatePayment() {
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(orderServiceClient.getOrderById(orderId)).willReturn(orderDto);
        given(paymentGateway.createPaymentIntent(BigDecimal.valueOf(99.99), "MAD", orderId))
                .willReturn(new PaymentIntentData("pi_test_123"));
        given(paymentMapper.toEntity(createRequest)).willReturn(Payment.builder().build());
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        given(paymentMapper.toDto(payment)).willReturn(paymentDto);

        PaymentDto result = paymentService.createPayment(userId, email, createRequest);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createPayment_shouldThrow_whenPaymentAlreadyExists() {
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createPayment(userId, email, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment already exists");
    }

    @Test
    void createPayment_shouldThrow_whenOrderNotFound() {
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(orderServiceClient.getOrderById(orderId)).willThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> paymentService.createPayment(userId, email, createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void getAll_shouldReturnPagedPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        given(paymentRepository.findAll(pageable)).willReturn(paymentPage);
        given(paymentMapper.toDto(payment)).willReturn(paymentDto);

        Page<PaymentDto> result = paymentService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getById_shouldReturnPayment() {
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(paymentMapper.toDto(payment)).willReturn(paymentDto);

        PaymentDto result = paymentService.getById(paymentId);

        assertThat(result).isEqualTo(paymentDto);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByOrderId_shouldReturnPayment() {
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
        given(paymentMapper.toDto(payment)).willReturn(paymentDto);

        PaymentDto result = paymentService.getByOrderId(orderId);

        assertThat(result).isEqualTo(paymentDto);
    }

    @Test
    void getByOrderId_shouldThrow_whenNotFound() {
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByOrderId(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatePaymentStatus_shouldUpdateStatus() {
        PaymentUpdateRequest updateDto = PaymentUpdateRequest.builder()
                .status(PaymentStatus.SUCCEEDED).build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        given(paymentMapper.toDto(payment)).willReturn(paymentDto);

        PaymentDto result = paymentService.updatePaymentStatus(paymentId, updateDto);

        assertThat(result).isNotNull();
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updatePaymentStatus_shouldThrow_whenNotFound() {
        PaymentUpdateRequest updateDto = PaymentUpdateRequest.builder()
                .status(PaymentStatus.SUCCEEDED).build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(paymentId, updateDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatePaymentStatus_shouldThrow_whenRefunded() {
        payment.setStatus(PaymentStatus.REFUNDED);
        PaymentUpdateRequest updateDto = PaymentUpdateRequest.builder()
                .status(PaymentStatus.PENDING).build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(paymentId, updateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("refunded");
    }

    @Test
    void handleWebhook_succeeded_shouldMarkAsSucceeded() {
        String payload = "{}";
        String sigHeader = "test_sig";
        WebhookEvent event = new WebhookEvent("payment_intent.succeeded", "pi_test_123");
        given(paymentGateway.constructWebhookEvent(payload, sigHeader)).willReturn(event);
        given(paymentRepository.findByStripePaymentIntentId("pi_test_123")).willReturn(Optional.of(payment));

        paymentService.handleWebhook(payload, sigHeader);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(paymentRepository).save(payment);
        verify(orderServiceClient).updateOrderStatus(orderId, OrderStatusUpdateDto.builder().status("CONFIRMED").build());
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleWebhook_failed_shouldMarkAsFailed() {
        String payload = "{}";
        String sigHeader = "test_sig";
        WebhookEvent event = new WebhookEvent("payment_intent.payment_failed", "pi_test_123");
        given(paymentGateway.constructWebhookEvent(payload, sigHeader)).willReturn(event);
        given(paymentRepository.findByStripePaymentIntentId("pi_test_123")).willReturn(Optional.of(payment));

        paymentService.handleWebhook(payload, sigHeader);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleWebhook_unhandled_shouldLogOnly() {
        String payload = "{}";
        String sigHeader = "test_sig";
        WebhookEvent event = new WebhookEvent("charge.succeeded", "pi_test_123");
        given(paymentGateway.constructWebhookEvent(payload, sigHeader)).willReturn(event);

        paymentService.handleWebhook(payload, sigHeader);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldRefund() {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .amount(BigDecimal.valueOf(99.99)).build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        given(paymentMapper.toDto(any(Payment.class))).willReturn(paymentDto);

        paymentService.refundPayment(paymentId, refundRequest);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentGateway).refundPayment("pi_test_123", BigDecimal.valueOf(99.99));
    }

    @Test
    void refundPayment_shouldThrow_whenAlreadyRefunded() {
        payment.setStatus(PaymentStatus.REFUNDED);
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder().build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, refundRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already fully refunded");
    }

    @Test
    void deletePayment_shouldDelete() {
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        paymentService.deletePayment(paymentId);

        verify(paymentRepository).delete(payment);
    }

    @Test
    void deletePayment_shouldThrow_whenSucceeded() {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.deletePayment(paymentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete completed payment");
    }

    @Test
    void deletePayment_shouldThrow_whenNotFound() {
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.deletePayment(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

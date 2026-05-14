package com.ecomera.payment.payment.service;

import com.ecomera.payment.client.OrderServiceClient;
import com.ecomera.payment.client.dto.OrderDto;
import com.ecomera.payment.client.dto.OrderStatusUpdateDto;
import com.ecomera.payment.payment.dto.PaymentCreateRequest;
import com.ecomera.payment.payment.dto.PaymentDto;
import com.ecomera.payment.payment.dto.PaymentRefundRequest;
import com.ecomera.payment.payment.dto.PaymentUpdateRequest;
import com.ecomera.payment.payment.entity.Payment;
import com.ecomera.payment.payment.enums.PaymentStatus;
import com.ecomera.payment.payment.mapper.PaymentMapper;
import com.ecomera.payment.payment.repository.PaymentRepository;
import com.ecomera.payment.shared.common.exception.BusinessException;
import com.ecomera.payment.shared.common.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGateway paymentGateway;
    private final OrderServiceClient orderServiceClient;

    @Value("${payment.default-currency:MAD}")
    private String defaultCurrency;

    @Transactional
    public PaymentDto createPayment(UUID userId, PaymentCreateRequest request) {
        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new BusinessException("Payment already exists for order: " + request.orderId());
        }

        PaymentIntentData intent;
        OrderDto order;
        try {
            order = orderServiceClient.getOrderById(request.orderId());
            intent = paymentGateway.createPaymentIntent(
                    order.totalPrice(), defaultCurrency, request.orderId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Order", "id", request.orderId());
        }

        Payment payment = paymentMapper.toEntity(request);
        payment.setUserId(userId);
        payment.setAmount(order.totalPrice());
        payment.setCurrency(defaultCurrency);
        payment.setStripePaymentIntentId(intent.id());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: {} for order: {} by user: {}",
                saved.getId(), request.orderId(), userId);
        return paymentMapper.toDto(saved);
    }

    public Page<PaymentDto> getAll(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
    }

    @Cacheable(value = "payments", key = "#id")
    public PaymentDto getById(UUID id) {
        log.debug("Cache miss fetching payment {} from DB", id);
        return paymentRepository.findById(id)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Payment.class, "id", id));
    }

    public PaymentDto getByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Payment.class, "orderId", orderId));
    }

    @Transactional
    @CachePut(value = "payments", key = "#id")
    public PaymentDto updatePaymentStatus(UUID id, PaymentUpdateRequest dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Payment.class, "id", id));

        PaymentStatus current = payment.getStatus();
        PaymentStatus target = dto.status();

        if (target != null) {
            validateStatusTransition(current, target);
        }

        paymentMapper.updateEntityFromDto(dto, payment);
        Payment saved = paymentRepository.save(payment);

        if (target != null && target != current) {
            syncOrderStatus(saved);
        }

        log.info("Payment {} updated", id);
        return paymentMapper.toDto(saved);
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        WebhookEvent event = paymentGateway.constructWebhookEvent(payload, sigHeader);

        if ("payment_intent.succeeded".equals(event.type())) {
            Payment payment = paymentRepository
                    .findByStripePaymentIntentId(event.paymentIntentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment not found for stripePaymentIntentId: " + event.paymentIntentId()));

            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as SUCCEEDED for intent: {}", payment.getId(), event.paymentIntentId());

            try {
                orderServiceClient.updateOrderStatus(
                        payment.getOrderId(),
                        OrderStatusUpdateDto.builder().status("CONFIRMED").build()
                );
                log.info("Order {} status updated to CONFIRMED via Feign", payment.getOrderId());
            } catch (Exception e) {
                log.error("Failed to update order {} status after payment success: {}",
                        payment.getOrderId(), e.getMessage());
            }

        } else if ("payment_intent.payment_failed".equals(event.type())) {
            Payment payment = paymentRepository
                    .findByStripePaymentIntentId(event.paymentIntentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment not found for stripePaymentIntentId: " + event.paymentIntentId()));

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as FAILED for intent: {}", payment.getId(), event.paymentIntentId());

        } else {
            log.debug("Unhandled webhook event type: {}", event.type());
        }
    }

    @Transactional
    @CachePut(value = "payments", key = "#id")
    public PaymentDto refundPayment(UUID id, PaymentRefundRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Payment.class, "id", id));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("Payment is already fully refunded");
        }

        paymentGateway.refundPayment(payment.getStripePaymentIntentId(), request.amount());

        if (request.amount() != null
                && request.amount().compareTo(payment.getAmount()) < 0) {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.REFUNDED);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} refunded. New status: {}", saved.getId(), saved.getStatus());
        return paymentMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "payments", key = "#id")
    public void deletePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Payment.class, "id", id));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new BusinessException("Cannot delete completed payment. Use refund instead.");
        }

        paymentRepository.delete(payment);
        log.info("Payment {} deleted", id);
    }

    private void syncOrderStatus(Payment payment) {
        String orderStatus = switch (payment.getStatus()) {
            case SUCCEEDED -> "CONFIRMED";
            case FAILED -> "CANCELLED";
            case REFUNDED -> "CANCELLED";
            default -> null;
        };

        if (orderStatus == null) return;

        try {
            orderServiceClient.updateOrderStatus(
                    payment.getOrderId(),
                    OrderStatusUpdateDto.builder().status(orderStatus).build()
            );
            log.info("Order {} status updated to {} via Feign", payment.getOrderId(), orderStatus);
        } catch (Exception e) {
            log.error("Failed to update order {} status after payment update: {}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    private void validateStatusTransition(PaymentStatus current, PaymentStatus target) {
        if (current == target) return;

        if (current == PaymentStatus.REFUNDED) {
            throw new BusinessException("Cannot change status of a refunded payment");
        }

        if (current == PaymentStatus.FAILED && target != PaymentStatus.PENDING) {
            throw new BusinessException("Failed payment can only be reset to PENDING");
        }

        if (current == PaymentStatus.SUCCEEDED
                && target != PaymentStatus.REFUNDED
                && target != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException("Completed payment can only be refunded");
        }
    }
}

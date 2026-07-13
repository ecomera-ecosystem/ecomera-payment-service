package com.ecomera.payment.payment.repository;

import com.ecomera.payment.payment.entity.Payment;
import com.ecomera.payment.payment.enums.PaymentMethod;
import com.ecomera.payment.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID orderId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        payment = Payment.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .amount(BigDecimal.valueOf(99.99))
                .currency("MAD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_test_" + UUID.randomUUID())
                .build();
        payment = entityManager.persistAndFlush(payment);
    }

    @Test
    void findByOrderId_shouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findByOrderId(orderId);
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void findByOrderId_shouldReturnEmpty_whenNotExists() {
        Optional<Payment> found = paymentRepository.findByOrderId(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findByStripePaymentIntentId_shouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findByStripePaymentIntentId(payment.getStripePaymentIntentId());
        assertThat(found).isPresent();
        assertThat(found.get().getStripePaymentIntentId()).isEqualTo(payment.getStripePaymentIntentId());
    }

    @Test
    void findByStripePaymentIntentId_shouldReturnEmpty_whenNotExists() {
        Optional<Payment> found = paymentRepository.findByStripePaymentIntentId("pi_nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void findById_shouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
    }

    @Test
    void findAll_shouldReturnPagedPayments() {
        Page<Payment> result = paymentRepository.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void save_shouldPersistPaymentWithAllFields() {
        Payment newPayment = Payment.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("another@example.com")
                .amount(BigDecimal.valueOf(50.00))
                .currency("USD")
                .paymentMethod(PaymentMethod.PAYPAL)
                .status(PaymentStatus.SUCCEEDED)
                .stripePaymentIntentId("pi_new_test")
                .build();

        Payment saved = entityManager.persistAndFlush(newPayment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(saved.getCurrency()).isEqualTo("USD");
    }
}

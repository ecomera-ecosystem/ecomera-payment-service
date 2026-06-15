package com.ecomera.payment.payment.controller;

import com.ecomera.payment.payment.dto.PaymentCreateRequest;
import com.ecomera.payment.payment.dto.PaymentDto;
import com.ecomera.payment.payment.dto.PaymentRefundRequest;
import com.ecomera.payment.payment.dto.PaymentUpdateRequest;
import com.ecomera.payment.payment.service.PaymentService;
import com.ecomera.payment.shared.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a payment intent")
    @ApiResponse(responseCode = "201", description = "Payment intent created")
    @ApiResponse(responseCode = "400", description = "Invalid payment data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PaymentDto> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(userId, email, request));
    }

    @GetMapping
    @Operation(summary = "Get all payments (paginated)")
    @ApiResponse(responseCode = "200", description = "Payments retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Page<PaymentDto>> getAll(
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        requireAdminOrManager(roles);
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(paymentService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    @ApiResponse(responseCode = "200", description = "Payment retrieved")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<PaymentDto> getById(
            @Parameter(description = "Payment UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    @ApiResponse(responseCode = "200", description = "Payment retrieved")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<PaymentDto> getByOrderId(
            @Parameter(description = "Order UUID") @PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update payment status")
    @ApiResponse(responseCode = "200", description = "Payment updated")
    @ApiResponse(responseCode = "400", description = "Invalid status transition")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<PaymentDto> update(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Payment UUID") @PathVariable UUID id,
            @Valid @RequestBody PaymentUpdateRequest request) {
        requireAdminOrManager(roles);
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment")
    @ApiResponse(responseCode = "204", description = "Payment deleted")
    @ApiResponse(responseCode = "400", description = "Cannot delete completed payment")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Payment UUID") @PathVariable UUID id) {
        requireAdminRole(roles);
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed")
    @ApiResponse(responseCode = "400", description = "Invalid webhook signature")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment")
    @ApiResponse(responseCode = "200", description = "Payment refunded")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<PaymentDto> refund(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Payment UUID") @PathVariable UUID id,
            @Valid @RequestBody(required = false) PaymentRefundRequest request) {
        requireAdminOrManager(roles);
        if (request == null) {
            request = PaymentRefundRequest.builder().build();
        }
        return ResponseEntity.ok(paymentService.refundPayment(id, request));
    }

    private void requireAdminOrManager(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()
                || (!rolesHeader.contains(ADMIN_ROLE) && !rolesHeader.contains(MANAGER_ROLE))) {
            throw new ApiException("Insufficient permissions. Requires ADMIN or MANAGER role.", HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()
                || !rolesHeader.contains(ADMIN_ROLE)) {
            throw new ApiException("Insufficient permissions. Requires ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}

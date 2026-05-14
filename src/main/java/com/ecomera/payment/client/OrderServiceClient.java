package com.ecomera.payment.client;

import com.ecomera.payment.client.dto.OrderDto;
import com.ecomera.payment.client.dto.OrderStatusUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "ecomera-order-service", path = "/api/v1/orders")
public interface OrderServiceClient {

    @GetMapping("/{orderId}")
    OrderDto getOrderById(@PathVariable("orderId") UUID orderId);

    @PatchMapping("/{orderId}")
    void updateOrderStatus(@PathVariable("orderId") UUID orderId, @RequestBody OrderStatusUpdateDto dto);
}

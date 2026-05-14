package com.ecomera.payment.payment.mapper;

import com.ecomera.payment.payment.dto.PaymentCreateRequest;
import com.ecomera.payment.payment.dto.PaymentDto;
import com.ecomera.payment.payment.dto.PaymentUpdateRequest;
import com.ecomera.payment.payment.entity.Payment;
import com.ecomera.payment.shared.common.mapper.BaseMappingConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMappingConfig.class)
public interface PaymentMapper {

    PaymentDto toDto(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stripePaymentIntentId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    Payment toEntity(PaymentCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stripePaymentIntentId", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PaymentUpdateRequest dto, @MappingTarget Payment payment);
}

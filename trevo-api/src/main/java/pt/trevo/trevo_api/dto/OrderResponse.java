package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;

public record OrderResponse(
    Long id,
    Long shopId,
    String shopName,
    OrderStatus status,
    String trackingNumber,
    BigDecimal totalAmount,
    BigDecimal commissionAmount,
    PaymentMethod paymentMethod,
    String paymentReference,
    Instant createdAt,
    Instant updatedAt,
    List<OrderItemResponse> items
) {}

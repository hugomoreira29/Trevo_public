package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long variantId,
    Long productId,
    String productName,
    String size,
    String color,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {}

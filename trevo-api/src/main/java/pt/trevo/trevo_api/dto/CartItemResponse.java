package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;

public record CartItemResponse(
    Long cartItemId,
    Long variantId,
    Long productId,
    String productName,
    String image,
    String size,
    String color,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {}

package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartShopGroupResponse(
    Long shopId,
    String shopName,
    List<CartItemResponse> items,
    BigDecimal subtotal
) {}

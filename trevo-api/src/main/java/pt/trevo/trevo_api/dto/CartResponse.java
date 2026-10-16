package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    List<CartShopGroupResponse> shops,
    BigDecimal grandTotal
) {}

package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;

public record ReceiptLineItem(
    String productName,
    String size,
    String color,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}

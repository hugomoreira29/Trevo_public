package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReceiptData(
    Long orderId,
    String buyerName,
    String buyerEmail,
    Instant issuedAt,
    List<ReceiptLineItem> items,
    BigDecimal total
) {
}

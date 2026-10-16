package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;

public record PayoutBalanceResponse(BigDecimal pendingBalance) {
}

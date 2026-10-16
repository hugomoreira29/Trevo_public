package pt.trevo.trevo_api.dto;

public record PaymentIntentResponse(
    String id,
    String clientSecret,
    long amount,
    String currency
) {
}

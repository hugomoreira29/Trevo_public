package pt.trevo.trevo_api.service;

public record StripePaymentIntentResult(
    String id,
    String clientSecret,
    long amount,
    String currency
) {
}

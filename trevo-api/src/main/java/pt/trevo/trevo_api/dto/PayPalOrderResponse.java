package pt.trevo.trevo_api.dto;

public record PayPalOrderResponse(
    String id,
    String status,
    String approveUrl
) {
}

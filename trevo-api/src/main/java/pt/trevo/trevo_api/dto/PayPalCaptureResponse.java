package pt.trevo.trevo_api.dto;

public record PayPalCaptureResponse(
    String id,
    String status
) {
}

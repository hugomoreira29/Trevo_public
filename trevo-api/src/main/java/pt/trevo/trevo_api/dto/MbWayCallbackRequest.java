package pt.trevo.trevo_api.dto;

public record MbWayCallbackRequest(
    String reference,
    String status
) {
}

package pt.trevo.trevo_api.dto;

public record UpdateReviewRequest(
    Integer rating,
    String comment
) {
}

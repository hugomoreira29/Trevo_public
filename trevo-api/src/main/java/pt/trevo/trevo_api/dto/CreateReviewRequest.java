package pt.trevo.trevo_api.dto;

public record CreateReviewRequest(
    Integer rating,
    String comment
) {
}

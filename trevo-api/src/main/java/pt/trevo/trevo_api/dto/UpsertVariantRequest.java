package pt.trevo.trevo_api.dto;

public record UpsertVariantRequest(
    String size,
    String color,
    Integer stock
) {
}

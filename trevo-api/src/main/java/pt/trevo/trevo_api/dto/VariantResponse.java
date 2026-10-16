package pt.trevo.trevo_api.dto;

public record VariantResponse(
    Long id,
    Long productId,
    String size,
    String color,
    Integer stock
) {}

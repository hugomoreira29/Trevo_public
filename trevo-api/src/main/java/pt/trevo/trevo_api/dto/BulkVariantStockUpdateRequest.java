package pt.trevo.trevo_api.dto;

public record BulkVariantStockUpdateRequest(
    Long variantId,
    Integer stock
) {
}

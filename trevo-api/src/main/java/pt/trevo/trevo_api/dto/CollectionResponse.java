package pt.trevo.trevo_api.dto;

public record CollectionResponse(
    Long id,
    Long shopId,
    String name,
    Integer displayOrder
) {
}

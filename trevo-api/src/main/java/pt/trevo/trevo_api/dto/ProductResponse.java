package pt.trevo.trevo_api.dto;

import pt.trevo.trevo_api.persistence.entity.GarmentType;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    Long shopId,
    String shopName,
    String name,
    String description,
    BigDecimal price,
    String brand,
    String materials,
    GarmentType garmentType,
    ProductStatus status,
    List<VariantResponse> variants,
    List<ImageResponse> images,
    LocalDateTime createdAt
) {

    public record ImageResponse(Long id, String imageUrl, Integer displayOrder) {}
}

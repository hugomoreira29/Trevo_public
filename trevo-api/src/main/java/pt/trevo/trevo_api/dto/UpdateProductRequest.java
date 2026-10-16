package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import pt.trevo.trevo_api.persistence.entity.GarmentType;

public record UpdateProductRequest(
    String name,
    String description,
    BigDecimal price,
    String brand,
    String materials,
    GarmentType garmentType
) {
}

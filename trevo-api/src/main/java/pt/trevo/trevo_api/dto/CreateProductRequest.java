package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import pt.trevo.trevo_api.persistence.entity.GarmentType;

public record CreateProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    String brand,
    String materials,
    @NotNull GarmentType garmentType
) {}

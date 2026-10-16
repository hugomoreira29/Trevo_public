package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCollectionRequest(
    @NotBlank(message = "Collection name is required")
    String name
) {
}

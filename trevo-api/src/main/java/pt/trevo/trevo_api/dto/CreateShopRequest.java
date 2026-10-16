package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(

    @NotBlank(message = "Shop name is required")
    String name,
    String description,
    @NotBlank(message = "NIF is required")
    String nif,
    String logoUrl,
    String bannerUrl
) {
}

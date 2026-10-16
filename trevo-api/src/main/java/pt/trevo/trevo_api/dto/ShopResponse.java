package pt.trevo.trevo_api.dto;

import pt.trevo.trevo_api.persistence.entity.ShopStatus;

public record ShopResponse(
    Long id,
    Long ownerId,
    String name,
    String description,
    String nif,
    String logoUrl,
    String bannerUrl,
    ShopStatus status
) {
}

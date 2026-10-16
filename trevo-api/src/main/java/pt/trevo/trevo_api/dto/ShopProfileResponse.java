package pt.trevo.trevo_api.dto;

import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import java.util.List;

public record ShopProfileResponse(
    Long id,
    Long ownerId,
    String name,
    String description,
    String logoUrl,
    String bannerUrl,
    ShopStatus status,
    Double averageRating,
    List<CollectionResponse> collections
) {
}

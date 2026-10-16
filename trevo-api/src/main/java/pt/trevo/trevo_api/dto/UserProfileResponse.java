package pt.trevo.trevo_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    Role role,
    Status status,
    String shopName,
    ShopStatus shopStatus
) {
}

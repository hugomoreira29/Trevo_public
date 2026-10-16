package pt.trevo.trevo_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterShopOwnerRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String fullName,
    String phone,
    @NotBlank String shopName,
    @NotBlank @Pattern(regexp = "^\\d{9}$") String nif
) {
}

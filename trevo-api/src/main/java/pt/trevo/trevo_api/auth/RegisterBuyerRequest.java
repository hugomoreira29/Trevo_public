package pt.trevo.trevo_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterBuyerRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String fullName,
    String phone
) {
}

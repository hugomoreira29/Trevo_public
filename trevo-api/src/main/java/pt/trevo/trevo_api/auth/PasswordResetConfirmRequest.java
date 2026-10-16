package pt.trevo.trevo_api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8) String newPassword
) {
}

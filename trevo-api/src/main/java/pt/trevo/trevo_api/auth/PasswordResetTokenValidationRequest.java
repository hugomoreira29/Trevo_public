package pt.trevo.trevo_api.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetTokenValidationRequest(@NotBlank String token) {
}

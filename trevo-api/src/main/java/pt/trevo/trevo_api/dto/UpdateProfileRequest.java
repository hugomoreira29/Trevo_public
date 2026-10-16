package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
    String fullName,
    @Email String email,
    String phone,
    String currentPassword,
    String newPassword
) {
}

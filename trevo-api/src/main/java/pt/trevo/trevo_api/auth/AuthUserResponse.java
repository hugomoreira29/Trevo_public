package pt.trevo.trevo_api.auth;

import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Status;

public record AuthUserResponse(Long id, String email, Role role, Status status) {
}

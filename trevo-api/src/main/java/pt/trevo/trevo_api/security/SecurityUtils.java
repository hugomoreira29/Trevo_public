package pt.trevo.trevo_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.Role;

@Component
public class SecurityUtils {

    public Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String value && isParsableUserId(value)) {
            return Long.valueOf(value);
        }

        String name = authentication.getName();
        if (isParsableUserId(name)) {
            return Long.valueOf(name);
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user id is unavailable");
    }

    public boolean hasRole(Authentication authentication, Role role) {
        if (authentication == null || role == null) {
            return false;
        }

        String authority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }

    private boolean isParsableUserId(String value) {
        return value != null && !value.isBlank() && !"anonymousUser".equals(value) && value.chars().allMatch(Character::isDigit);
    }
}

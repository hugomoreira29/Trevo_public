package pt.trevo.trevo_api.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.persistence.entity.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/buyer")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserResponse registerBuyer(@Valid @RequestBody RegisterBuyerRequest request) {
        return toResponse(authService.registerBuyer(request));
    }

    @PostMapping("/register/shop-owner")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserResponse registerShopOwner(@Valid @RequestBody RegisterShopOwnerRequest request) {
        return toResponse(authService.registerShopOwner(request));
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/sessions/invalidate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSessions(Authentication authentication) {
        authService.invalidateSessions(authentication);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-reset/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validatePasswordResetToken(@Valid @RequestBody PasswordResetTokenValidationRequest request) {
        authService.validatePasswordResetToken(request.token());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }
}

package pt.trevo.trevo_api.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.PasswordResetToken;
import pt.trevo.trevo_api.persistence.entity.RefreshToken;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.PasswordResetTokenRepository;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetEmailSender passwordResetEmailSender;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;
    private final AuthProperties authProperties;

    public AuthService(
        UserRepository userRepository,
        ShopRepository shopRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        PasswordResetEmailSender passwordResetEmailSender,
        AuthenticationManager authenticationManager,
        Clock clock,
        AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetEmailSender = passwordResetEmailSender;
        this.authenticationManager = authenticationManager;
        this.clock = clock;
        this.authProperties = authProperties;
    }

    @Transactional
    public User registerBuyer(RegisterBuyerRequest request) {
        requireBuyerFields(request);
        ensureEmailIsAvailable(request.email());

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizeOptional(request.phone()));
        user.setRole(Role.BUYER);
        user.setStatus(Status.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User registerShopOwner(RegisterShopOwnerRequest request) {
        requireShopOwnerFields(request);
        ensureEmailIsAvailable(request.email());
        ensureNifIsAvailable(request.nif());

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizeOptional(request.phone()));
        user.setRole(Role.SHOP_OWNER);
        user.setStatus(Status.PENDING);
        User savedUser = userRepository.save(user);

        Shop shop = new Shop();
        shop.setOwner(savedUser);
        shop.setName(request.shopName().trim());
        shop.setNif(request.nif().trim());
        shop.setStatus(ShopStatus.PENDING);
        shopRepository.save(shop);

        return savedUser;
    }

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        requireLoginFields(request);
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw unauthorized("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> unauthorized("Invalid email or password"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateAndPersistRefreshToken(user);
        return new TokenPairResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenPairResponse refresh(RefreshTokenRequest request) {
        requireRefreshToken(request);
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
            .orElseThrow(() -> unauthorized("Invalid refresh token"));

        if (Boolean.TRUE.equals(storedToken.getRevoked())) {
            throw unauthorized("Invalid refresh token");
        }
        if (!storedToken.getExpiresAt().isAfter(clock.instant())) {
            storedToken.setRevoked(true);
            throw unauthorized("Refresh token expired");
        }

        storedToken.setRevoked(true);
        User user = storedToken.getUser();
        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateAndPersistRefreshToken(user);
        return new TokenPairResponse(accessToken, refreshToken);
    }

    @Transactional
    public void invalidateSessions(Authentication authentication) {
        refreshTokenRepository.revokeAllByUserId(extractAuthenticatedUserId(authentication));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (isBlank(email)) {
            throw badRequest("Missing email");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElse(null);

        if (user == null) {
            return;
        }

        Instant now = clock.instant();

        boolean withinCooldown = passwordResetTokenRepository
            .findTopByUserOrderByCreatedAtDesc(user)
            .map(t -> t.getCreatedAt().plus(authProperties.passwordResetCooldown()).isAfter(now))
            .orElse(false);
        if (withinCooldown) {
            return;
        }

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(authProperties.passwordResetTokenExpiration()));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);
        passwordResetEmailSender.sendPasswordResetEmail(user, rawToken);
    }

    @Transactional(readOnly = true)
    public void validatePasswordResetToken(String token) {
        getValidPasswordResetToken(token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (isBlank(newPassword)) {
            throw badRequest("Missing password");
        }

        PasswordResetToken resetToken = getValidPasswordResetToken(token);
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsed(true);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private void requireBuyerFields(RegisterBuyerRequest request) {
        if (request == null
            || isBlank(request.email())
            || isBlank(request.password())
            || isBlank(request.fullName())) {
            throw badRequest("Missing required fields");
        }
    }

    private void requireShopOwnerFields(RegisterShopOwnerRequest request) {
        if (request == null
            || isBlank(request.email())
            || isBlank(request.password())
            || isBlank(request.fullName())
            || isBlank(request.shopName())
            || isBlank(request.nif())) {
            throw badRequest("Missing required fields");
        }
        if (!request.nif().trim().matches("^\\d{9}$")) {
            throw badRequest("NIF must contain exactly 9 digits");
        }
    }

    private void requireLoginFields(LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw badRequest("Missing required fields");
        }
    }

    private void requireRefreshToken(RefreshTokenRequest request) {
        if (request == null || isBlank(request.refreshToken())) {
            throw badRequest("Missing refresh token");
        }
    }

    private void ensureEmailIsAvailable(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
    }

    private void ensureNifIsAvailable(String nif) {
        if (shopRepository.findByNif(nif.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NIF already exists");
        }
    }

    private String generateAndPersistRefreshToken(User user) {
        Instant now = clock.instant();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(authProperties.refreshTokenExpiration()));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private PasswordResetToken getValidPasswordResetToken(String token) {
        if (isBlank(token)) {
            throw badRequest("Missing reset token");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashToken(token))
            .orElseThrow(() -> badRequest("Invalid password reset token"));

        if (!resetToken.getExpiresAt().isAfter(clock.instant())) {
            throw badRequest("Password reset token expired");
        }

        return resetToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private Long extractAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw unauthorized("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(principal.toString());
        } catch (NumberFormatException exception) {
            throw unauthorized("Authentication required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }
}

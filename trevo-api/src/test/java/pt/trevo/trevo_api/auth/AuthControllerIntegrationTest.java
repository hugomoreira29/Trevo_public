package pt.trevo.trevo_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.persistence.entity.PasswordResetToken;
import pt.trevo.trevo_api.persistence.entity.RefreshToken;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.PasswordResetTokenRepository;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@Import(AuthControllerIntegrationTest.TestBuyerController.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private PasswordResetEmailSender passwordResetEmailSender;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void cleanDatabase() {
        rateLimitingFilter.resetBuckets();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        shopRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buyerRegistrationEndpointReturnsCreatedAndPersistsBuyer() throws Exception {
        RegisterBuyerRequest request = new RegisterBuyerRequest("buyer@example.com", "Secret123!", "Buyer Name", "912345678");

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("buyer@example.com"))
            .andExpect(jsonPath("$.role").value("BUYER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        User savedUser = userRepository.findByEmail("buyer@example.com").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.BUYER);
        assertThat(savedUser.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(savedUser.getPasswordHash()).startsWith("$2");
    }

    @Test
    void buyerRegistrationEndpointReturnsConflictForDuplicateEmail() throws Exception {
        userRepository.save(User.builder()
            .email("buyer@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Existing Buyer")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());

        RegisterBuyerRequest request = new RegisterBuyerRequest("buyer@example.com", "Secret123!", "Buyer Name", "912345678");

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void buyerRegistrationEndpointReturnsBadRequestForMissingFields() throws Exception {
        RegisterBuyerRequest request = new RegisterBuyerRequest("", "", "", null);

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shopOwnerRegistrationEndpointReturnsCreatedAndPersistsPendingOwnerAndShop() throws Exception {
        RegisterShopOwnerRequest request = new RegisterShopOwnerRequest(
            "shop@example.com",
            "Secret123!",
            "Shop Owner",
            "912345678",
            "Trevo Boutique",
            "123456789"
        );

        mockMvc.perform(post("/api/auth/register/shop-owner")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("shop@example.com"))
            .andExpect(jsonPath("$.role").value("SHOP_OWNER"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        User savedUser = userRepository.findByEmail("shop@example.com").orElseThrow();
        Shop savedShop = shopRepository.findByOwnerId(savedUser.getId()).orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.SHOP_OWNER);
        assertThat(savedUser.getStatus()).isEqualTo(Status.PENDING);
        assertThat(savedShop.getNif()).isEqualTo("123456789");
    }

    @Test
    void shopOwnerRegistrationEndpointReturnsBadRequestForInvalidNif() throws Exception {
        RegisterShopOwnerRequest request = new RegisterShopOwnerRequest(
            "shop@example.com",
            "Secret123!",
            "Shop Owner",
            "912345678",
            "Trevo Boutique",
            "12345"
        );

        mockMvc.perform(post("/api/auth/register/shop-owner")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginEndpointReturnsTokenPairAndPersistsRefreshToken() throws Exception {
        String email = "login@example.com";
        String password = "Secret123!";
        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterBuyerRequest(email, password, "Login User", "912345678"))))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn();

        TokenPairResponse response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), TokenPairResponse.class);
        RefreshToken savedToken = refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken()).orElseThrow();
        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(savedToken.getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void loginEndpointReturnsUnauthorizedForWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterBuyerRequest(
                    "login-wrong@example.com",
                    "Secret123!",
                    "Login User",
                    "912345678"
                ))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("login-wrong@example.com", "wrong-password"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpointReturnsUnauthorizedForMissingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("missing@example.com", "Secret123!"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpointReturnsNewAccessTokenAndRotatesRefreshToken() throws Exception {
        String email = "refresh@example.com";
        String password = "Secret123!";
        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterBuyerRequest(email, password, "Refresh User", "912345678"))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();
        TokenPairResponse loginResponse =
            objectMapper.readValue(loginResult.getResponse().getContentAsByteArray(), TokenPairResponse.class);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(loginResponse.refreshToken()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn();

        TokenPairResponse refreshResponse =
            objectMapper.readValue(refreshResult.getResponse().getContentAsByteArray(), TokenPairResponse.class);

        assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(loginResponse.refreshToken())).isEmpty();
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshResponse.refreshToken())).isPresent();
    }

    @Test
    void refreshEndpointReturnsUnauthorizedForExpiredRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
            .email("expired-refresh@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Expired Refresh")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("expired-refresh-token");
        token.setExpiresAt(java.time.Instant.now().minusSeconds(60));
        token.setRevoked(false);
        token.setCreatedAt(java.time.Instant.now().minusSeconds(120));
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest("expired-refresh-token"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpointReturnsUnauthorizedForRevokedRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
            .email("revoked-refresh@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Revoked Refresh")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("revoked-refresh-token");
        token.setExpiresAt(java.time.Instant.now().plusSeconds(600));
        token.setRevoked(true);
        token.setCreatedAt(java.time.Instant.now().minusSeconds(120));
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest("revoked-refresh-token"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidateSessionsRevokesAllRefreshTokensAndLeavesAccessTokenValidUntilExpiry() throws Exception {
        String email = "invalidate@example.com";
        String password = "Secret123!";

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterBuyerRequest(email, password, "Invalidate User", "912345678"))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();

        TokenPairResponse loginResponse =
            objectMapper.readValue(loginResult.getResponse().getContentAsByteArray(), TokenPairResponse.class);

        mockMvc.perform(post("/api/auth/sessions/invalidate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.accessToken()))
            .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(loginResponse.refreshToken())).isEmpty();
        assertThat(refreshTokenRepository.findAll())
            .extracting(RefreshToken::getRevoked)
            .containsOnly(true);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(loginResponse.refreshToken()))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/buyer/test/secure")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.accessToken()))
            .andExpect(status().isOk());
    }

    @Test
    void passwordResetRequestEndpointPersistsTokenWithFifteenMinuteExpiryAndSendsEmail() throws Exception {
        User user = userRepository.save(User.builder()
            .email("reset@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Reset User")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest("reset@example.com"))))
            .andExpect(status().isNoContent());

        PasswordResetToken token = passwordResetTokenRepository.findAll().stream().findFirst().orElseThrow();
        String rawToken = captureRawResetToken();
        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        assertThat(token.getTokenHash()).isEqualTo(hashToken(rawToken));
        assertThat(token.getUsed()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
        assertThat(token.getExpiresAt()).isEqualTo(token.getCreatedAt().plusSeconds(15 * 60));
        verify(passwordResetEmailSender).sendPasswordResetEmail(
            argThat(savedUser -> savedUser != null && "reset@example.com".equals(savedUser.getEmail())),
            eq(rawToken)
        );
    }

    @Test
    void passwordResetRequestEndpointForUnknownEmailReturnsNoContentWithoutSendingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest("unknown@example.com"))))
            .andExpect(status().isNoContent());

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(passwordResetEmailSender, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void passwordResetRequestEndpointInvalidatesPreviousResetTokensForUser() throws Exception {
        User user = userRepository.save(User.builder()
            .email("repeat-reset@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Repeat Reset")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        PasswordResetToken existingToken = new PasswordResetToken();
        existingToken.setUser(user);
        existingToken.setTokenHash(hashToken("old-reset-token"));
        existingToken.setUsed(false);
        existingToken.setCreatedAt(java.time.Instant.now().minusSeconds(60));
        existingToken.setExpiresAt(java.time.Instant.now().plusSeconds(300));
        passwordResetTokenRepository.saveAndFlush(existingToken);

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest("repeat-reset@example.com"))))
            .andExpect(status().isNoContent());

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);

        mockMvc.perform(post("/api/auth/password-reset/validate")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetTokenValidationRequest("old-reset-token"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetValidateEndpointReturnsNoContentForValidToken() throws Exception {
        User user = userRepository.save(User.builder()
            .email("validate-reset@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Validate Reset")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken("valid-reset-token"));
        token.setUsed(false);
        token.setCreatedAt(java.time.Instant.now());
        token.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        passwordResetTokenRepository.saveAndFlush(token);

        mockMvc.perform(post("/api/auth/password-reset/validate")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetTokenValidationRequest("valid-reset-token"))))
            .andExpect(status().isNoContent());
    }

    @Test
    void passwordResetConfirmEndpointUpdatesPasswordAndMarksTokenAsUsed() throws Exception {
        String oldPassword = "OldSecret123!";
        String newPassword = "NewSecret123!";

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterBuyerRequest(
                    "confirm-reset@example.com",
                    oldPassword,
                    "Confirm Reset",
                    "912345678"
                ))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("confirm-reset@example.com", oldPassword))))
            .andExpect(status().isOk())
            .andReturn();
        TokenPairResponse loginResponse =
            objectMapper.readValue(loginResult.getResponse().getContentAsByteArray(), TokenPairResponse.class);

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest("confirm-reset@example.com"))))
            .andExpect(status().isNoContent());

        PasswordResetToken token = passwordResetTokenRepository.findAll().stream().findFirst().orElseThrow();
        String rawToken = captureRawResetToken();

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(rawToken, newPassword))))
            .andExpect(status().isNoContent());

        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updatedToken.getUsed()).isTrue();
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(loginResponse.refreshToken())).isEmpty();

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("confirm-reset@example.com", oldPassword))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("confirm-reset@example.com", newPassword))))
            .andExpect(status().isOk());
    }

    @Test
    void passwordResetValidateEndpointReturnsBadRequestForExpiredToken() throws Exception {
        User user = userRepository.save(User.builder()
            .email("expired-reset@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Expired Reset")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken("expired-reset-token"));
        token.setUsed(false);
        token.setCreatedAt(java.time.Instant.now().minusSeconds(120));
        token.setExpiresAt(java.time.Instant.now().minusSeconds(60));
        passwordResetTokenRepository.saveAndFlush(token);

        mockMvc.perform(post("/api/auth/password-reset/validate")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetTokenValidationRequest("expired-reset-token"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetValidateEndpointReturnsBadRequestForMissingOrUsedToken() throws Exception {
        User user = userRepository.save(User.builder()
            .email("used-reset@example.com")
            .passwordHash("$2a$10$existingHash")
            .fullName("Used Reset")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build());
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken("used-reset-token"));
        token.setUsed(true);
        token.setCreatedAt(java.time.Instant.now().minusSeconds(60));
        token.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        passwordResetTokenRepository.saveAndFlush(token);

        mockMvc.perform(post("/api/auth/password-reset/validate")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetTokenValidationRequest("used-reset-token"))))
            .andExpect(status().isBadRequest());
    }

    private String captureRawResetToken() {
        org.mockito.ArgumentCaptor<String> tokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).sendPasswordResetEmail(any(), tokenCaptor.capture());
        return tokenCaptor.getValue();
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    @RestController
    @RequestMapping("/api/buyer/test")
    static class TestBuyerController {

        @GetMapping("/secure")
        String secureEndpoint() {
            return "buyer";
        }
    }
}

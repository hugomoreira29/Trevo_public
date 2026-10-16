package pt.trevo.trevo_api.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.HttpStatus;
import pt.trevo.trevo_api.TrevoApiApplication;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.CartRepository;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.NotificationRepository;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.PasswordResetTokenRepository;
import pt.trevo.trevo_api.persistence.repository.PayoutRepository;
import pt.trevo.trevo_api.persistence.repository.ProductImageRepository;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ReviewRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.SubscriptionRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnImageRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnUsageRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;
import pt.trevo.trevo_api.service.AiImageGenerationClient;
import pt.trevo.trevo_api.service.UserPhotoService;

@SpringBootTest(
        classes = TrevoApiApplication.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        }
)
@TestPropertySource(properties = {
        // Provide a dummy JWT secret long enough for HS256 (≥256 bits / 32 chars)
        "app.security.jwt.secret=test-secret-key-for-junit-testing-only-minimum-32chars",
        "app.security.jwt.expiration=PT1H"
})
@Import({
        SecurityConfigIntegrationTest.TestAuthController.class,
        SecurityConfigIntegrationTest.TestShopController.class,
        SecurityConfigIntegrationTest.TestBuyerController.class
})
class SecurityConfigIntegrationTest {

    @MockitoBean
    private ProductImageRepository productImageRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private VariantRepository variantRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CollectionRepository collectionRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private PayoutRepository payoutRepository;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private UserPhotoService userPhotoService;

    @MockitoBean
    private TryOnImageRepository tryOnImageRepository;

    @MockitoBean
    private TryOnUsageRepository tryOnUsageRepository;

    @MockitoBean
    private AiImageGenerationClient aiImageGenerationClient;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authEndpointsShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/auth/test/public"))
                .andExpect(status().isOk());
    }

    @Test
    void sessionInvalidationEndpointShouldRejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/sessions/invalidate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionInvalidationEndpointShouldAllowAuthenticatedUsers() throws Exception {
        String token = generateToken(14L, Role.BUYER);

        mockMvc.perform(post("/api/auth/sessions/invalidate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void shopEndpointsShouldRequireShopOwnerRole() throws Exception {
        String token = generateToken(10L, Role.SHOP_OWNER);

        mockMvc.perform(get("/api/shops/test/secure")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shopEndpointsShouldRejectBuyerRole() throws Exception {
        String token = generateToken(11L, Role.BUYER);

        mockMvc.perform(get("/api/shops/test/secure")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shopEndpointsShouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/shops/test/secure"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shopEndpointsShouldAllowAdmins() throws Exception {
        String token = generateToken(14L, Role.ADMIN);

        mockMvc.perform(get("/api/shops/test/secure")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void buyerEndpointsShouldRejectShopOwners() throws Exception {
        String token = generateToken(12L, Role.SHOP_OWNER);

        mockMvc.perform(get("/api/buyer/test/secure")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void buyerEndpointsShouldAllowBuyers() throws Exception {
        String token = generateToken(13L, Role.BUYER);

        mockMvc.perform(get("/api/buyer/test/secure")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String generateToken(Long userId, Role role) {
        return jwtService.generateToken(User.builder()
                .id(userId)
                .role(role)
                .build());
    }

    @RestController
    @RequestMapping("/api/auth/test")
    static class TestAuthController {

        @GetMapping("/public")
        String publicEndpoint() {
            return "auth";
        }

        // Stub for the real invalidate endpoint so the security rule has a handler
        @PostMapping("/sessions/invalidate")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void invalidateSession() {
            // no-op stub — security is what's under test
        }
    }

    @RestController
    @RequestMapping("/api/shops/test")
    static class TestShopController {

        @GetMapping("/secure")
        String secureEndpoint() {
            return "shop";
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

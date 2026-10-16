package pt.trevo.trevo_api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.TrevoApiApplication;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.*;
import pt.trevo.trevo_api.service.AiImageGenerationClient;

@SpringBootTest(
    classes = TrevoApiApplication.class,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    }
)
@Import(JwtAuthenticationFilterIntegrationTest.TestProtectedController.class)
class JwtAuthenticationFilterIntegrationTest {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductImageRepository productImageRepository;

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
    private UserPhotoRepository userPhotoRepository;

    @MockitoBean
    private TryOnImageRepository tryOnImageRepository;

    @MockitoBean
    private TryOnUsageRepository tryOnUsageRepository;

    @MockitoBean
    private AiImageGenerationClient aiImageGenerationClient;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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
    void requestWithValidTokenShouldReturn200() throws Exception {
        String token = jwtService.generateToken(User.builder()
            .id(42L)
            .role(Role.BUYER)
            .build());

        mockMvc.perform(get("/api/test/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"authenticated\""));
    }

    @Test
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @RestController
    @RequestMapping("/api/test/protected")
    static class TestProtectedController {

        @GetMapping
        String protectedEndpoint() {
            return "authenticated";
        }
    }
}

package pt.trevo.trevo_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
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
import pt.trevo.trevo_api.persistence.repository.UserPhotoRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;
import pt.trevo.trevo_api.service.AiImageGenerationClient;

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    }
)
class RateLimitingFilterIntegrationTest {

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ShopRepository shopRepository;
    @MockitoBean private CartRepository cartRepository;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockitoBean private ReviewRepository reviewRepository;
    @MockitoBean private ProductRepository productRepository;
    @MockitoBean private ProductImageRepository productImageRepository;
    @MockitoBean private VariantRepository variantRepository;
    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private PayoutRepository payoutRepository;
    @MockitoBean private CollectionRepository collectionRepository;
    @MockitoBean private NotificationRepository notificationRepository;
    @MockitoBean private UserPhotoRepository userPhotoRepository;
    @MockitoBean private SubscriptionRepository subscriptionRepository;
    @MockitoBean private TryOnImageRepository tryOnImageRepository;
    @MockitoBean private TryOnUsageRepository tryOnUsageRepository;
    @MockitoBean private AiImageGenerationClient aiImageGenerationClient;

    @Autowired private WebApplicationContext context;
    @Autowired private RateLimitingFilter rateLimitingFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.resetBuckets();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void eleventhRequestWithinOneMinuteReturns429() throws Exception {
        String body = "{\"email\":\"test@test.com\",\"password\":\"wrong\"}";

        for (int i = 0; i < RateLimitingFilter.MAX_REQUESTS_PER_MINUTE; i++) {
            int status = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
            assertThat(status).isNotEqualTo(429);
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void nonAuthEndpointsAreNotRateLimited() throws Exception {
        for (int i = 0; i < RateLimitingFilter.MAX_REQUESTS_PER_MINUTE + 5; i++) {
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        }
    }
}

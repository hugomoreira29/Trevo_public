package pt.trevo.trevo_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=" +
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
		"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
		"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class TrevoApiApplicationTests {

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
	private ProductImageRepository productImageRepository;

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

	@Test
	void contextLoads() {
	}

}

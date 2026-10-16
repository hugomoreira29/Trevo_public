package pt.trevo.trevo_api.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import pt.trevo.trevo_api.persistence.entity.Cart;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderItem;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.PasswordResetToken;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.RefreshToken;
import pt.trevo.trevo_api.persistence.entity.Review;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.Subscription;
import pt.trevo.trevo_api.persistence.entity.SubscriptionStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.flyway.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryDataJpaTest {

    private static int userSequence = 0;
    private static int shopSequence = 0;
    private static int productSequence = 0;

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("trevo_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void shouldPersistCartAndFindItByBuyerId() {
        User buyer = persistUser();

        Cart cart = new Cart();
        cart.setBuyer(buyer);

        Cart savedCart = cartRepository.saveAndFlush(cart);

        assertThat(savedCart.getId()).isNotNull();
        assertThat(cartRepository.findByBuyerIdWithItems(buyer.getId()))
                .isPresent()
                .get()
                .extracting(Cart::getId)
                .isEqualTo(savedCart.getId());
    }

    @Test
    void shouldPersistOrderAndFindItByBuyerAndShop() {
        User buyer = persistUser();
        Shop shop = persistShop();
        Variant variant = persistVariant();

        Order firstOrder = buildOrder(buyer, shop, variant, Instant.parse("2026-04-01T10:15:30Z"), OrderStatus.PLACED);
        Order secondOrder = buildOrder(buyer, shop, variant, Instant.parse("2026-04-02T10:15:30Z"), OrderStatus.SHIPPED);

        orderRepository.save(firstOrder);
        orderRepository.saveAndFlush(secondOrder);

        List<Order> byBuyer = orderRepository.findAllByBuyerIdOrderByCreatedAtDesc(buyer.getId());
        List<Order> byShopAndStatus = orderRepository.findAllByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), OrderStatus.SHIPPED);

        assertThat(byBuyer).hasSize(2);
        assertThat(byBuyer).extracting(Order::getStatus).containsExactly(OrderStatus.SHIPPED, OrderStatus.PLACED);
        assertThat(byShopAndStatus).hasSize(1);
        assertThat(byShopAndStatus.getFirst().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldPersistReviewAndExposeCustomQueries() {
        User buyerOne = persistUser();
        User buyerTwo = persistUser();
        Shop shop = persistShop();

        Review olderReview = new Review();
        olderReview.setBuyer(buyerOne);
        olderReview.setShop(shop);
        olderReview.setRating(4);
        olderReview.setComment("Great experience");
        olderReview.setCreatedAt(Instant.parse("2026-04-01T09:00:00Z"));
        olderReview.setUpdatedAt(Instant.parse("2026-04-01T09:00:00Z"));

        Review newerReview = new Review();
        newerReview.setBuyer(buyerTwo);
        newerReview.setShop(shop);
        newerReview.setRating(5);
        newerReview.setComment("Excellent service");
        newerReview.setCreatedAt(Instant.parse("2026-04-02T09:00:00Z"));
        newerReview.setUpdatedAt(Instant.parse("2026-04-02T09:00:00Z"));

        reviewRepository.save(olderReview);
        reviewRepository.saveAndFlush(newerReview);

        assertThat(reviewRepository.existsByBuyerIdAndShopId(buyerOne.getId(), shop.getId())).isTrue();
        assertThat(reviewRepository.findAllByShopIdOrderByCreatedAtDesc(shop.getId()))
                .extracting(Review::getRating)
                .containsExactly(5, 4);
        assertThat(reviewRepository.findAverageRatingByShopId(shop.getId())).isEqualTo(4.5d);
    }

    @Test
    void shouldPersistSubscriptionAndFindItByBuyerAndStripeId() {
        User buyer = persistUser();

        Subscription subscription = new Subscription();
        subscription.setBuyer(buyer);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStripeSubscriptionId("sub_123");
        subscription.setStartDate(Instant.parse("2026-04-01T00:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-01T00:00:00Z"));
        subscription.setCreatedAt(Instant.parse("2026-04-01T00:00:00Z"));

        Subscription savedSubscription = subscriptionRepository.saveAndFlush(subscription);

        assertThat(savedSubscription.getId()).isNotNull();
        assertThat(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(buyer.getId())).isPresent();
        assertThat(subscriptionRepository.findByStripeSubscriptionId("sub_123")).isPresent();
    }

    @Test
    void shouldPersistRefreshTokenAndSupportSessionInvalidation() {
        User user = persistUser();

        RefreshToken activeToken = new RefreshToken();
        activeToken.setUser(user);
        activeToken.setToken("refresh-active");
        activeToken.setExpiresAt(Instant.parse("2026-05-01T00:00:00Z"));
        activeToken.setRevoked(false);
        activeToken.setCreatedAt(Instant.parse("2026-04-01T00:00:00Z"));

        RefreshToken secondToken = new RefreshToken();
        secondToken.setUser(user);
        secondToken.setToken("refresh-second");
        secondToken.setExpiresAt(Instant.parse("2026-05-02T00:00:00Z"));
        secondToken.setRevoked(false);
        secondToken.setCreatedAt(Instant.parse("2026-04-02T00:00:00Z"));

        refreshTokenRepository.save(activeToken);
        refreshTokenRepository.saveAndFlush(secondToken);

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-active")).isPresent();

        int updatedRows = refreshTokenRepository.revokeAllByUserId(user.getId());
        entityManager.clear();

        assertThat(updatedRows).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-active")).isEmpty();
        assertThat(refreshTokenRepository.findAll())
                .extracting(RefreshToken::getRevoked)
                .containsOnly(true);
    }

    @Test
    void shouldPersistPasswordResetTokenAndFindOnlyUnusedTokens() {
        User user = persistUser();

        PasswordResetToken activeToken = new PasswordResetToken();
        activeToken.setUser(user);
        activeToken.setTokenHash("hash-active");
        activeToken.setExpiresAt(Instant.parse("2026-04-10T00:00:00Z"));
        activeToken.setUsed(false);
        activeToken.setCreatedAt(Instant.parse("2026-04-01T00:00:00Z"));

        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setUser(user);
        usedToken.setTokenHash("hash-used");
        usedToken.setExpiresAt(Instant.parse("2026-04-11T00:00:00Z"));
        usedToken.setUsed(true);
        usedToken.setCreatedAt(Instant.parse("2026-04-02T00:00:00Z"));

        passwordResetTokenRepository.save(activeToken);
        passwordResetTokenRepository.saveAndFlush(usedToken);

        assertThat(passwordResetTokenRepository.findByTokenHashAndUsedFalse("hash-active")).isPresent();
        assertThat(passwordResetTokenRepository.findByTokenHashAndUsedFalse("hash-used")).isEmpty();
    }

    private Order buildOrder(User buyer, Shop shop, Variant variant, Instant createdAt, OrderStatus status) {
        Order order = new Order();
        order.setBuyer(buyer);
        order.setShop(shop);
        order.setStatus(status);
        order.setTrackingNumber(status == OrderStatus.SHIPPED ? "TRACK-123" : null);
        order.setTotalAmount(new BigDecimal("149.99"));
        order.setCommissionAmount(new BigDecimal("14.99"));
        order.setPaymentMethod(PaymentMethod.STRIPE);
        order.setPaymentReference("pi_123");
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setVariant(variant);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("74.99"));
        order.getItems().add(item);
        return order;
    }

    private User persistUser() {
        User user = new User();
        userSequence++;
        user.setEmail("user" + userSequence + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setFullName("Test User " + userSequence);
        user.setRole(Role.BUYER);
        user.setStatus(Status.ACTIVE);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Shop persistShop() {
        shopSequence++;
        Shop shop = new Shop();
        shop.setOwner(persistUser());
        shop.setName("Test Shop " + shopSequence);
        shop.setNif(String.format("%09d", shopSequence));
        shop.setStatus(ShopStatus.ACTIVE);
        entityManager.persist(shop);
        entityManager.flush();
        return shop;
    }

    private Variant persistVariant() {
        productSequence++;
        Product product = new Product();
        product.setShop(persistShop());
        product.setName("Test Product " + productSequence);
        product.setPrice(new BigDecimal("49.99"));
        product.setStatus(ProductStatus.ACTIVE);
        entityManager.persist(product);

        Variant variant = new Variant();
        variant.setProduct(product);
        variant.setSize("M");
        variant.setColor("Black");
        variant.setStock(10);
        entityManager.persist(variant);
        entityManager.flush();
        return variant;
    }
}
